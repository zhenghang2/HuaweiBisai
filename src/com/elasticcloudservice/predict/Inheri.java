package com.elasticcloudservice.predict;
import java.io.BufferedReader;  
import java.io.FileInputStream;  
import java.io.IOException;  
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;  
  
public class Inheri {  
  
    private int scale;  
    private int Num;   
    private int MAX_GEN;   
    private double bestLen; 
    private int[] bestSeq; 
   
    private int[][] oldSeq;  
    private int[][] newSeq;
    private double[] fitness;  
  
    private float[] Pro;  
    private float Pc;  
    private float Pm; 
    private int t;
    
    private int[] CPU;
    private int[] MEM;
    
    public  HashMap<Integer, Integer> map;
  
    private int CPU_NUM;
    
    private int MEM_NUM;
    
    
    private Random random;  
  
    public Inheri(int s, int n, int g, float c, float m,int CPU_NUM,int MEM_NUM,int[] CPU,int[] MEM,HashMap<Integer, Integer> map) {  
        this.scale = s;  
        this.Num = n;  
        this.MAX_GEN = g;  
        this.Pc = c;  
        this.Pm = m;  
        this.CPU_NUM = CPU_NUM;
        this.MEM_NUM = MEM_NUM;
        this.CPU = CPU;
        this.MEM = MEM;
        this.map = map;
    }  
  
    @SuppressWarnings("resource")  
    public void init(){    
  
        bestLen = Double.MAX_VALUE;  
        
        bestSeq = new int[Num];  
        t = 0;  
  
        newSeq = new int[scale][Num];  
        oldSeq = new int[scale][Num];  
        fitness = new double[scale];  
        Pro = new float[scale];  
  
        random = new Random(System.currentTimeMillis());  
        
    }  
   
    void initGroup() {  
        int i, n, m;    
        for (m = 0; m < scale; m++)  
        {  
            oldSeq[m][0] = random.nextInt(65535) % Num;  
            for (i = 1; i < Num;)  
            {  
                oldSeq[m][i] = random.nextInt(65535) % Num;  
                for (n = 0; n < i; n++) {  
                    if (oldSeq[m][i] == oldSeq[m][n]) {  
                        break;  
                    }  
                }  
                if (n == i) {  
                    i++;  
                }  
            }  
        }  
    
    }  
  
    public double evaluate(int[] chromosome) {  

    	double len = 0;  
        int[] NEW_CPU = new int[Num];
        int[] NEW_MEM = new int[Num];
 
        for (int i = 0; i < Num; i++) {  
        	NEW_CPU[i] = CPU[chromosome[i]];
        	NEW_MEM[i] = MEM[chromosome[i]];  
        }  
        len = count(NEW_CPU, NEW_MEM);

        return len;  
    }  
 
    void Rate() {  
        int k;  
        double sum = 0;
  
        double[] temps = new double[scale];  
  
        for (k = 0; k < scale; k++) {  
        	temps[k] = 10.0 / fitness[k];  
        	sum += temps[k];  
        }  
  
        Pro[0] = (float) (temps[0] / sum);  
        for (k = 1; k < scale; k++) {  
            Pro[k] = (float) (temps[k] / sum + Pro[k - 1]);  
        }  
   
    }  
   
    public void selectBestSeq() {  
        int k, i, id;  
        double maxValue;  
  
        id = 0;  
        maxValue = Double.MAX_VALUE;  
        for (k = 1; k < scale; k++) {  
            if (maxValue > fitness[k] && fitness[k] != 0) { 
            	maxValue = fitness[k]; 
                id = k;  
            }  
        }  
  
        if (bestLen > maxValue) {  
            bestLen = maxValue;   
            for (i = 0; i < Num; i++) {  
                bestSeq[i] = oldSeq[id][i];  
            }  
        }  

        copy(0, id); 
    }  
  

    public void copy(int cur, int old_cur) {   
        for (int i = 0; i < Num; i++) {  
            newSeq[cur][i] = oldSeq[old_cur][i];  
        }  
    }  
  

    public void select() {  
        int k, sc, selectId;  
        float pro_cur;  
        
        for (k = 1; k < scale; k++) {  
            pro_cur = (float) (random.nextInt(65535) % 1000 / 1000.0);  
 
            for (sc = 0; sc < scale; sc++) {  
                if (pro_cur <= Pro[sc]) {  
                    break;  
                }  
            }  
            if(sc == scale) sc -= 1;
            selectId = sc;  
              
            copy(k, selectId);  
        }  
    }  
  
      
    public void evolution() {  
        int k;  

        selectBestSeq();  
 
        select();  
   
        float pro_cur;  
  
         
        for (k = 0; k < scale; k = k + 2) {  
        	pro_cur = random.nextFloat();
 
            if (pro_cur < Pc) {  

                Across(k, k + 1);  
            } else {  
            	pro_cur = random.nextFloat();  

                if (pro_cur < Pm) {  

                    Swap(k);  
                }  
                pro_cur = random.nextFloat();  
            
                if (pro_cur < Pm) {  
                    
                    Swap(k + 1);  
                }  
            }  
  
        }  
    }  
    
    //进化函数，保留最好染色体不进行交叉变异  
    public void evolution1() {  
        int k;  
         
        selectBestSeq();  
    
        select();  
    
        float r;  
  
        for (k = 1; k + 1 < scale / 2; k = k + 2) {  
            r = random.nextFloat();
            if (r < Pc) {  
                Across(k, k + 1);  

            } else {  
                r = random.nextFloat();
                 
                if (r < Pm) {  
                    Swap(k);  
                }  
                r = random.nextFloat(); 
                  
                if (r < Pm) {  
                    Swap(k + 1);  
                }  
            }  
        }  
        if (k == scale / 2 - 1)  
        {  
            r = random.nextFloat();
            if (r < Pm) {  
                Swap(k);  
            }  
        }  
  
    } 
  
    public void Across(int k1, int k2) {  
        int i, j, k, flag;  
        int pro1, pro2, temp;  
        int[] Chromosome1 = new int[Num];  
        int[] Chromosome2 = new int[Num];    
  
        pro1 = random.nextInt(65535) % Num;  
        pro2 = random.nextInt(65535) % Num;  
        while (pro1 == pro2) {  
            pro2 = random.nextInt(65535) % Num;  
        }  
  
        if (pro1 > pro2)  
        {  
            temp = pro1;  
            pro1 = pro2;  
            pro2 = temp;  
        }  

        for (i = 0, j = pro2; j < Num; i++, j++) {  
            Chromosome2[i] = newSeq[k1][j];  
        }  
  
        flag = i;  
  
        for (k = 0, j = flag; j < Num;) 
        {  
            Chromosome2[j] = newSeq[k2][k++];  
            for (i = 0; i < flag; i++) {  
                if (Chromosome2[i] == Chromosome2[j]) {  
                    break;  
                }  
            }  
            if (i == flag) {  
                j++;  
            }  
        }  
  
        flag = pro1;  
        for (k = 0, j = 0; k < Num;)  
        {  
            Chromosome1[j] = newSeq[k1][k++];  
            for (i = 0; i < flag; i++) {  
                if (newSeq[k2][i] == Chromosome1[j]) {  
                    break;  
                }  
            }  
            if (i == flag) {  
                j++;  
            }  
        }  
  
        flag = Num - pro1;  
  
        for (i = 0, j = flag; j < Num; j++, i++) {  
            Chromosome1[j] = newSeq[k2][i];  
        }  
  
        for (i = 0; i < Num; i++) {  
            newSeq[k1][i] = Chromosome1[i];
            newSeq[k2][i] = Chromosome2[i];  
        }  
    }  
  
    public void Swap(int k) {  
        int pro1, pro2, temp;  
        int count;  
  

        count = random.nextInt(65535) % Num;  
  
        for (int i = 0; i < count; i++) {  
  
            pro1 = random.nextInt(65535) % Num;  
            pro2 = random.nextInt(65535) % Num;  
            while (pro1 == pro2) {  
                pro2 = random.nextInt(65535) % Num;  
            }  
            temp = newSeq[k][pro1];  
            newSeq[k][pro1] = newSeq[k][pro2];  
            newSeq[k][pro2] = temp;  
        }  

    }  
  
    public int solve() {  
    	int server_count = 1;
    	
        int i;  
        int k;  

        initGroup();  
        
        Rate();  

          
        for (t = 0; t < MAX_GEN; t++) {  
              
            evolution1();  
 
            for (k = 0; k < scale; k++) {  
                for (i = 0; i < Num; i++) {  
                    oldSeq[k][i] = newSeq[k][i];  
                }  
            }  
 
            for (k = 0; k < scale; k++) {  
                fitness[k] = evaluate(oldSeq[k]);  
            }  

            Rate();  
        }  
    
        for (i = 0; i < Num; i++) {  
            System.out.print(bestSeq[i] + ",");  
        }  

        while (true) {
        	HashMap<Integer, Integer> countMap = new HashMap<>();
        	
   	        int size = 0;
   	        
   	        int[] NEW_CPU = new int[Num];
   	        int[] NEW_MEM = new int[Num];
   	        
   	        for (int j = 0; j < Num; j++) {  

   	        	NEW_CPU[j] = CPU[bestSeq[j]];
   	        	NEW_MEM[j] = MEM[bestSeq[j]];
   	        }
   	        
   	     List<Object> list = split(NEW_CPU, NEW_MEM);
   	     int len = (int) list.get(list.size() - 1);
   	     if(len == 1) {
   	    	countMap = new HashMap<>();
   	    	for (int j = 0; j < Num; j++) {
   	    		if(countMap.get(map.get(bestSeq[j])) == null) {
                	countMap.put(map.get(bestSeq[j]), 1);
                }else {
                	int time = countMap.get(map.get(bestSeq[j]));
                	countMap.put(map.get(bestSeq[j]), ++time);
                }
   	    	}
   	    	put(countMap, server_count);
   	    	
   	    	return server_count;
   	     }else {
			for(int j = 0;j < len;j++) {
				countMap = new HashMap<>();
				server_count = j+1;
				if(j == 0) {
				   int fir = 0;
				   int split = (int) list.get(j);
				   for(;fir < split;fir++) {
					   if(!countMap.containsKey(map.get(bestSeq[fir]))) {
						   countMap.put(map.get(bestSeq[fir]), 1);
					   }else {
						   int time = countMap.get(map.get(bestSeq[fir]));
						   countMap.put(map.get(bestSeq[fir]), ++time);
					}
				   }
				   put(countMap, server_count);
				}else if(j == len - 1){
					countMap = new HashMap<>();
					int fir = (int) list.get(j - 1);
					for(;fir < Num;fir++) {
						if(!countMap.containsKey(map.get(bestSeq[fir]))) {
							   countMap.put(map.get(bestSeq[fir]), 1);
						   }else {
							   int time = countMap.get(map.get(bestSeq[fir]));
							   countMap.put(map.get(bestSeq[fir]), ++time);
						}
					}
					put(countMap, server_count);
					
					return server_count;
				}else {
					countMap = new HashMap<>();
					int fir = (int) list.get(j - 1);
					int next = (int) list.get(j);
					
					for(;fir < next;fir++) {
						if(countMap.get(map.get(bestSeq[fir])) == null) {
							   countMap.put(map.get(bestSeq[fir]), 1);
						   }else {
							   int time = countMap.get(map.get(bestSeq[fir]));
							   countMap.put(map.get(bestSeq[fir]), ++time);
						}
					}
					
					put(countMap, server_count);
				}
			}
		  }
		}
    }  
    
    //返回需要的物理机数量
    private double count(int[] CPU,int[] MEM) {
    	double total_cpu = 0;
    	double total_mem = 0;
    	List<Double> list = new ArrayList<>();
    	
    	double count = 0;
    	int cpuNum = CPU_NUM;
    	int memNum = MEM_NUM;
    	
    	List<Object> lists = split(CPU, MEM);
    	int len = (int) lists.get(lists.size() - 1);
    	if(len == 1) {
    		return 1;
    	}else {
			int fir = (int) lists.get(len - 2);
			for(;fir < Num;fir++) {
				if(Predict.type.equals("CPU")) {
					total_cpu += CPU[fir];
				}
				if(Predict.type.equals("MEM")) {
					total_mem += MEM[fir];
				}
			}
			
			if(Predict.type.equals("CPU")) {
				count = total_cpu / CPU_NUM + len - 1;
				return count;
			}
			
			if(Predict.type.equals("MEM")) {
				count = total_mem / MEM_NUM  + len - 1;
				return count;
			}
		}
    	
    	return count;
    }  
    
    private List<Object> split(int[] CPU,int[] MEM){
    	List<Object> list = new ArrayList<>();
    	int count = 1;
    	int cpuNum = CPU_NUM;
    	int memNum = MEM_NUM;
    	
    	for(int i = 0;i < Num;i++) {
    		if(cpuNum >= CPU[i] && memNum >= MEM[i]) {
    			cpuNum -= CPU[i];
    			memNum -= MEM[i];
    		}else {
    			list.add(i);
				count++;
				cpuNum = CPU_NUM; 
				memNum = MEM_NUM;
				cpuNum -= CPU[i];
    			memNum -= MEM[i];
			}
    	}
    	list.add(count);
    	return list;
    	
    }
    
    private void put(HashMap<Integer, Integer> countMap,int server_count) {
    	Predict.server[server_count][Constants.FREE_CPU] = CPU_NUM;
		Predict.server[server_count][Constants.FREE_MEM] = MEM_NUM;
	    	
	    	for(Integer integer : countMap.	keySet()) {
        	Predict.server[server_count][integer] = countMap.get(integer);
        	
        	Predict.server[server_count][Constants.FREE_CPU] -= countMap.get(integer) * Predict.cpu_type[integer];
			Predict.server[server_count][Constants.FREE_MEM] -= countMap.get(integer) * Predict.mem_type[integer];
        }
    } 
    
 
} 
