package reducer;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

 

public class MatrixReducer extends Reducer<IntWritable, Text, Text, Text> {

    private int totalSize, partSize, npart;
    int[][] left=null;
    int[][] right=null;
    protected void setup(Context context) throws IOException, InterruptedException{
        //get how # of partitions
        Configuration configuration=context.getConfiguration();
        totalSize=configuration.getInt("matrix-mul-totalsize", -1);  //Getting 
        partSize=configuration.getInt("matrix-mul-partsize", -1);
        npart=configuration.getInt("matrix-mul-npart", -1);
        if(totalSize<0 || partSize<0 || npart<0){
                System.out.println("Error in setup of MyReducer.");
                System.exit(1);
        }
        left=new int[partSize][totalSize];
        right=new int[totalSize][partSize];
    }
    public void reduce(IntWritable key, Iterable<Text> values, Context context
                       ) throws IOException, InterruptedException {
        int sum = 0;
        for (Text val : values) {
                String line=val.toString();
                String[] meta_val=line.split("#");
                String[] metas=meta_val[0].split(":");
                String[] numbers=meta_val[1].split(" ");

                int baselinenum=Integer.parseInt(metas[1]);
                int blkindex=Integer.parseInt(metas[2]);
                if("l".equalsIgnoreCase(metas[0])){ //from left matrix
                        int start=blkindex * partSize;
                        for(int i=0;i<partSize; i++)
                                left[baselinenum][start+i]=Integer.parseInt(numbers[i]);
                }else{
                        int rowindex=blkindex*partSize + baselinenum;
                        for(int i=0;i<partSize; i++)
                                right[rowindex][i]=Integer.parseInt(numbers[i]);
                }
        }
    }
    protected void cleanup(Context context) throws IOException, InterruptedException {
        //now let's do the calculation
        int[][] res=new int[partSize][partSize];
        for(int i=0;i<partSize;i++)
                for(int j=0;j<partSize;j++)
                        res[i][j]=0;
        for(int i=0;i<partSize;i++){
                for(int k=0;k<totalSize;k++){
                        for(int j=0;j<partSize;j++){
                                res[i][j]+=left[i][k]*right[k][j];
                        }
                }
        }
        for(int i=0;i<partSize;i++){
                String output=null;
                for(int j=0;j<partSize;j++){
                        if(output==null)
                                output=""+res[i][j];
                        else
                                output+=" "+res[i][j];
                }
                context.write(new Text(output), null);
        }
    }
  }