package com.elasticcloudservice.predict;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.crypto.Data;

public class Predict {
	public static int[] cpu_type = {0,1,1,1,2,2,2,4,4,4,8,8,8,16,16,16,32,32,32,32};
	public static int[] mem_type = {0,1,2,4,2,4,8,4,8,16,8,16,32,16,32,64,32,64,128};
	//虚拟机类型列表
	public static ArrayList<String> flavor_type_list = new ArrayList<String>();//需要预测的虚拟机类型列表
	
	public static int Server_Count;
	public static ArrayList<String> Server_Name = new ArrayList<>();
	public static int[] Server_CPU = new int[3];
	public static int[] Server_MEM = new int[3];
	
	//用二维数组表示每台服务器上每种虚拟机类型的数量,server[i][j]代表第i台服务器上第j类虚拟机的数量
	public static int[][] server = new int[1000][22];
	public static int[] flavors_count = new int[Constants.FLAVORS_COUNT];//各个类型虚拟机的数量
	public static int[] flavors_count2 = new int[Constants.FLAVORS_COUNT];
	
	public static int CPU = 0;
	public static int MEM = 0;
	
	public static String type;

	public static HashMap<Integer, Integer> map = new HashMap<>();
	
	public static String[] predictVm(String[] ecsContent, String[] inputContent) {

		/** =========do your work here========== **/

		String[] results = new String[ecsContent.length];

		List<String> history = new ArrayList<String>();
		Map<String, Integer[]> container = new HashMap<>();
		//解析Input文件的map
		Map<String, Object> parseMap = new HashMap<>();
		parseMap = parseInputContent(inputContent);
		
		flavor_type_list  = (ArrayList<String>)parseMap.get("flavor_type_list");
		
		CPU = (int) parseMap.get(Constants.INPUT_PHYSICAL_CPU);
		MEM = (int) parseMap.get(Constants.INPUT_PHYSICAL_MEM);

		type = (String) parseMap.get(Constants.INPUT_PHYSICAL_RESOURCES_TYPE);
		
		String first  = "";
		String last = "";
		for (int i = 0; i < ecsContent.length; i++) {
			
				String[] array = ecsContent[i].split("\\s|\t");
				
				String flavor = array[1].substring(6);
                String date = array[2];
                history.add(flavor+" "+date);
                if(i == 0){
    				first = array[2];
    			}
    			if( i == ecsContent.length - 1){
    				last = array[2];
    				parseMap.put("last_train_day", last);
    			}
                
		}
		//计算训练结束与预测开始的间隔时间
		int inteval_day = caculateDate((String)parseMap.get("last_train_day"), (String)parseMap.get("startDate"))-1;
		//按照一个星期一个时刻，将总时间分组
		//int num = (caculateDate(first, last)+1) % 7 == 0 ? (caculateDate(first, last)+1) / 7 : (caculateDate(first, last)+1) / 7 + 1;
		int num = (caculateDate(first, last)+1)+inteval_day;
		//初始化容器
		initMap(container, num);
		//填充容器数据
		saveDataToMap(container, ecsContent, first,num);
		//预测间隔数据，并加入训练集中
		savapredictData(container,inteval_day,num);
		// 去除异常点
		dealData(container);
		int predict_data = (int) parseMap.get("predict_days");
		int numWeek = num % predict_data == 0
						? num / predict_data : num / predict_data + 1;
		newData(container, numWeek,predict_data);
		for(int i=1;i<=18;i++){
			Integer[] flavor = container.get(String.valueOf(i));
			for (Integer integer : flavor) {
				System.out.print(integer+" ");
			}
			System.out.println();
		}
		// 1-15个虚拟机，数量集合
		List<Integer> result = new ArrayList<>();
		//第一个位置设置0
		result.add(0);
		for(int a=1;a<=18;a++){
			Integer[] flavor = container.get(String.valueOf(a));
			double standar = getStandardDeviation(flavor);
			double module = getModule(standar);
			List<Double> data = new ArrayList<>();
			for(int i=0;i<flavor.length;i++){
				data.add((double)flavor[i]);
			}
			double value = getExpect(data, 1,module);
			int predict = (int)value;
			result.add(predict);
		}
		for (Integer integer : result) {
			System.out.print(integer+",");
		}
		System.out.println();
		
		//放置部分
		int all=0;
		Arrays.fill(flavors_count, 0);
		for (String flavor : flavor_type_list) {
			int flavorId = Integer.valueOf(flavor.substring(6));
			flavors_count[flavorId] = result.get(flavorId);
			flavors_count2[flavorId] = result.get(flavorId);
			all = all+result.get(flavorId);
		}
		
        int len = 0;
		for(int i = 1;i < flavors_count.length;i++) {
			if(flavor_type_list.contains("flavor" + String.valueOf(i))) {
				len += flavors_count[i];
			}
		}
		
		int[] weight = new int[len];
		int[] profit = new int[len];
		int index = 0;
		for(int i = 1;i < flavors_count.length;i++) {
			if(flavors_count[i] != 0 && flavor_type_list.contains("flavor" + String.valueOf(i))) {
				for(int t = 0;t < flavors_count[i];t++) {
					weight[index] = mem_type[i];
					profit[index] = cpu_type[i];
					map.put(index, i);
					index++;
				}
			}
		}
		
		//System.out.println(Server_CPU[0] + "  " + Server_MEM[0]);
		Inheri ga = new Inheri(30, weight.length, 2000, 0.8f, 0.1f,Server_CPU[0],Server_MEM[0], profit,weight,map);
		ga.init();
		int[] flavor_num = new int[20];
		Arrays.fill(flavor_num, 0);
		int server_count = ga.solve();

		int add_count = 0;
		add_count = fillFreeServers(server, flavors_count, server_count);

		//results数组赋值
				int k=1;
           String name = Server_Name.get(0);
				for (String flavor : flavor_type_list) {
					int flavorId = Integer.valueOf(flavor.substring(6));
					results[k]="flavor" + flavorId + " " + flavors_count[flavorId];
					k++;
				}
				results[0] = String.valueOf(all + add_count);
				//results[0] = String.valueOf(all);
				results[k++] = "";
				int temp = k;
				results[temp] = name + " "+String.valueOf(server_count);
				for(int i=1;i<=server_count;i++){
					results[temp+i] = name + "-"+String.valueOf(i);
					for(int j=1;j<=18;j++){
						if(server[i][j]!=0){
							results[temp+i] += " flavor"+j + " " + server[i][j];
						}
					}
				}


		return results;
	}

	//计算时间差值
	public static int caculateDate(String first,String current) {
		String time1 = current;  //第二个日期

		String time2 = first;  //第一个日期
		//算两个日期间隔多少天
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		Date date1;
		Date date2;
		int a=0;
		try {
			date1 = format.parse(time1);
			date2 = format.parse(time2);
			a = (int) ((date1.getTime() - date2.getTime()) / (1000*3600*24));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return a;
	}
	public static void initMap(Map<String, Integer[]> container,int num){
		for(int i=1;i<= 18;i++){
			Integer[] nums = new Integer[num];
			Arrays.fill(nums, 0);
			container.put(String.valueOf(i), nums);
		}
	}
	
	public static void saveDataToMap(Map<String, Integer[]> dataContainer, String[] ecsContent, String firstDate,
			int len) {
		for (int i = 0; i < ecsContent.length; i++) {
			String[] array = ecsContent[i].split("\\s|\t");
			String flavor = array[1].substring(6);
			String curDate = array[2];
			if (dataContainer.containsKey(flavor)) {
				Integer[] curArray = dataContainer.get(flavor);
				// 根据具体情况划分时刻,这里按照1天划分
				int time = caculateDate(firstDate, curDate);
				curArray[time]++;
			}
		}	
	}
	private static void savapredictData(Map<String, Integer[]> container, int inteval_day,int num) {
		int tem = num-inteval_day;
		double predict=0;
		for(int i=1;i<=18;i++){
			Integer[] curArray = container.get(String.valueOf(i));
			List<Double> data = new ArrayList<>();
			for(int k = 0;k<curArray.length-inteval_day;k++){
				data.add((double)curArray[k]);
			}
			for(int j =tem;j<num;j++){
				predict = getExpect(data, 1, 0.22)*1.55;
				if(predict<0){
					predict = 0;
				}
				curArray[j]=(int)predict;
				data.add(predict);
			}
		}
	}
	public static void getInput(String[] inputContent,int physicalserver_CPU,int physicalserver_Memory,List<Integer> flavor,int time){
		String[] firstline = inputContent[0].split(" ");
		physicalserver_CPU=Integer.parseInt(firstline[0]);
		physicalserver_Memory=Integer.parseInt(firstline[1]);
		
		int num = Integer.parseInt(inputContent[2]);
		flavor = new ArrayList<>();
		for(int i=0;i<num;i++){
			String [] array = inputContent[3+i].split(" ");
			flavor.add(Integer.parseInt(array[0].substring(6)));
		}
		String[] first =inputContent[6+num].split(" ");
		String[] last = inputContent[7+num].split(" ");
		time = caculateDate(first[0], last[0]);
	}
	public static Map<String, Object> parseInputContent(String[] inputContent){
//		Map<String, Object> map = new HashMap<String, Object>();
		
       Map<String, Object> map = new HashMap<String, Object>();
		
		int phy_count = Integer.parseInt(inputContent[0]);
		map.put("phy_count", phy_count);
		
		String[] phy = inputContent[1].split(" ");
		map.put("phy_cpu", Integer.parseInt(phy[1]));
		map.put("phy_mem", Integer.parseInt(phy[2]));
		for(int i = 1;i <= phy_count;i++) {
			String[] phy1 = inputContent[i].split(" ");

			Server_Name.add(phy1[0]);
			Server_CPU[i-1] = Integer.parseInt(phy1[1]);
			Server_MEM[i-1] = Integer.parseInt(phy1[2]);
		}
		
//		int phy_count = Integer.parseInt(inputContent[0]);
//		map.put("phy_count", phy_count);
//		String[] phy1 = inputContent[1].split(" ");
//		map.put("phy_cpu", Integer.parseInt(phy1[1]));
//		map.put("phy_mem", Integer.parseInt(phy1[2]));
//		String[] phy2 = inputContent[2].split(" ");
//		map.put("phy2_cpu", Integer.parseInt(phy2[1]));
//		map.put("phy2_mem", Integer.parseInt(phy2[2]));
//		String[] phy3 = inputContent[3].split(" ");
//		map.put("phy3_cpu", Integer.parseInt(phy3[1]));
//		map.put("phy3_mem", Integer.parseInt(phy3[2]));
		
			
		int flavor_count = Integer.parseInt(inputContent[5]);
		map.put("flavor_count", flavor_count);
		
		ArrayList<String> flavor_type_list = new ArrayList<String>();
		int i = 6;
		while((flavor_count--) > 0) {
			String flavor_type = inputContent[i].split(" ")[0];
			flavor_type_list.add(flavor_type);
			i++;
		}
		map.put("flavor_type_list",flavor_type_list);
		
		i++;
		map.put("phy_resources_type", "CPU");
		//i++;
		
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		try {
			map.put("startDate", inputContent[i].substring(0, 10));
			Date startDate = format.parse(inputContent[i++]);
			map.put("endData", inputContent[i].substring(0, 10));
			Date endData = format.parse(inputContent[i]);
			int predict_days = daysBetween(startDate, endData);
			int weekNum=1;
			int trainDays = weekNum*7;
			map.put("train_days", trainDays);
			map.put("predict_days", predict_days);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return map;
	}
	private static int daysBetween(Date start, Date end) {  
	    int difference =  (int) ((end.getTime()-start.getTime())/86400000);  
	    return difference;
	}
	private static boolean inFlavorList(int flavor_id) {
		String flavorName = "flavor"+flavor_id;
		if(flavor_type_list.contains(flavorName))
			return true;
		else
			return false;
	}
	
	private static int fillFreeServers(int[][] server,int[] count,int server_count) {
		int add_count=0;
		int total = 0;
		for(int j=1;j<=server_count;j++) {//遍历每台物理机
				for(int i=18;i>=1;i--) {//遍历每种类型的虚拟机;从小到大
					if(inFlavorList(i)) { //判断是不是预测的虚拟机
						if(cpu_type[i]<=server[j][Constants.FREE_CPU] && mem_type[i]<=server[j][Constants.FREE_MEM]) {
							if((add_count + flavors_count2[i]) >=  flavors_count2[i] * 2)
							{
								add_count = 0;
								continue;
							}
							count[i]++;
							add_count++;
							total++;
							server[j][Constants.FREE_CPU] -= cpu_type[i];
							server[j][Constants.FREE_MEM] -= mem_type[i];
							server[j][i]++;
							
							i++;//可加可不加 需要重新评估
							
						}
					}
				}
		}
		return total;
	}
	//计算次高值
	public static int caculateSecondNum(Integer[] num) {
		int max = 0;
		int second = 0;
		for (int i = 0; i < num.length; i++) {
			if (num[i] > max) {
				second = max;
				max = num[i];
				continue;
			}
			if (num[i] > second) {
				second = num[i];
			}
		}
		return second;
	}
	//去除异常点
	public static void dealData(Map<String, Integer[]> dataContainer) {
		for (int i = 1; i <= 18; i++) {
			Integer[] curArray = dataContainer.get(String.valueOf(i));
			//double abnormal = percentitle(curArray, 0.98);
			int second = caculateSecondNum(curArray);
			for (int j = 0; j < curArray.length; j++) {
				if (curArray[j] > 1.5 * second) {
					curArray[j] = (int)(1*second);
				}
			}

			dataContainer.put(String.valueOf(i), curArray);
		}
	}
	
	public static void newData(Map<String, Integer[]> dataContainer, int num,int predict) {
		for (int i = 1; i <= 18; i++) {
			Integer[] curArray = dataContainer.get(String.valueOf(i));
			Integer[] updateArray = new Integer[num];
			Arrays.fill(updateArray, 0);
			int len = curArray.length - 1;
			for (int j = 0; j < curArray.length; j++) {
				int index = (len - j) / predict;
				updateArray[num - index - 1] += curArray[j];
			}
			// 数组累加处理
			/*
			 * for (int j = 1; j < updateArray.length; j++) { updateArray[j] =
			 * updateArray[j - 1] + updateArray[j]; }
			 */
			// 滑窗大小
			/*
			 * int period = 3; Integer[] forcast = new
			 * Integer[updateArray.length - period]; Arrays.fill(forcast, 0);
			 * movingaverage(updateArray, forcast, period);
			 */
			dataContainer.put(String.valueOf(i), updateArray);
		}
	}
	
	/**
	 * 求平均值
	 * 
	 * @param x 数据
	 * @return 平均值
	 */

	public static double getAverage(Integer[] x) {
		int m = x.length;
		double sum = 0;
		for (int i = 0; i < m; i++) {// 求和
			sum += x[i];
		}
		double average = sum / m;// 求平均值
		return average;
	}
	
	/**
	 * 求标准差
	 * 
	 * @param x历史数据
	 * @return 标准差
	 */
	public static double getStandardDeviation(Integer[] x) {
		double dAve = getAverage(x);// 求平均值
		double dVar = 0;

		for (int i = 0; i < x.length; i++) {
			dVar += (x[i] - dAve) * (x[i] - dAve);
		}
		return Math.sqrt(dVar / x.length);
	}
	
	private static double getModule(double standar) {
		double module = 0;
		if (standar < 2) {
			module = 0.25;
		} else if (standar < 5) {
			module = 0.2;
		} else if (standar > 5 && standar < 10) {
			module = 0.22;
		} else if (standar > 10) {
			module = 0.23;
		}
		return module;
	}
	
	/**
     * 二次指数平滑法
     * @param list 历史数据
     * @param year 1
     * @param modulus 平滑系数
     * @return 预测值
     */
    private static Double getExpect(List<Double> list, int year, double modulus ) {
        /*if (list.size() < 10 || modulus <= 0 || modulus >= 1) {
            return null;
        }*/
        double modulusfirst = 1 - modulus;
        double lastIndex = list.get(0);
        double secIndex = list.get(0);
        for (double data :list) {
            lastIndex = modulus * data + modulusfirst * lastIndex;
            secIndex = modulus * lastIndex + modulusfirst * secIndex;
        }
        double i = 2 * lastIndex - secIndex;
        double j = (modulus / modulusfirst) * (lastIndex - secIndex);
        return i + j * year;
    }


	
}
class Constants {
	
	//input文件读取的内容
	public static final String INPUT_PHYSICAL_CPU = "phy_cpu";
	public static final String INPUT_PHYSICAL_MEM = "phy_mem";
	public static final String INPUT_PHYSICAL2_CPU= "phy2_cpu";
	public static final String INPUT_PHYSICAL2_MEM= "phy2_mem";
	public static final String INPUT_PHYSICAL3_CPU= "phy3_cpu";
	public static final String INPUT_PHYSICAL3_MEM= "phy3_mem";
	public static final String INPUT_FLAVORS_COUNT = "flavor_count";
	public static final String INPUT_FLAVORS_LIST = "flavor_type_list";
	public static final String INPUT_PHYSICAL_RESOURCES_TYPE = "phy_resources_type";
	public static final String INPUT_DAYS = "predict_days";
	public static final String INPUT_START_DATE = "startDate";
	public static final String INPUT_END_DATE = "endDate";
	
	public static final int FLAVOR1 = 1;
	public static final int FLAVOR2 = 2;
	public static final int FLAVOR3 = 3;
	public static final int FLAVOR4 = 4;
	public static final int FLAVOR5 = 5;
	public static final int FLAVOR6 = 6;
	public static final int FLAVOR7 = 7;
	public static final int FLAVOR8 = 8;
	public static final int FLAVOR9 = 9;
	public static final int FLAVOR10 = 10;
	public static final int FLAVOR11 = 11;
	public static final int FLAVOR12 = 12;
	public static final int FLAVOR13 = 13;
	public static final int FLAVOR14 = 14;
	public static final int FLAVOR15 = 15;
	public static final int FLAVOR16 = 16;
	public static final int FLAVOR17 = 17;
	public static final int FLAVOR18 = 18;
	
	public static final int FREE_CPU = 19;
	public static final int FREE_MEM = 20;
	
	public static final int FLAVORS_COUNT = 19;
}

