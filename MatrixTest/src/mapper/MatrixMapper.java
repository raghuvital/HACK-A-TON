package mapper;

import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;

  public class MatrixMapper extends Mapper<LongWritable, Text, IntWritable, Text>{

    private String filename=null;
    private boolean isLeftMatrix=false;
    private int totalSize, partSize, npart;

    private boolean isLeft(){return isLeftMatrix;}
    protected void setup(Context context) throws IOException, InterruptedException{
        //getting input files from input folder supplied 
        FileSplit fileSplit = (FileSplit)context.getInputSplit();
        filename = fileSplit.getPath().getName();
        
        //Checking the file LeftMatrix whether it exists or not
        if("left".equalsIgnoreCase(filename))
                isLeftMatrix=true;
        else
                isLeftMatrix=false;

        //get how size and partition information
        Configuration configuration=context.getConfiguration();
        totalSize=configuration.getInt("matrix-mul-totalsize", -1);
        partSize=configuration.getInt("matrix-mul-partsize", -1);
        npart=configuration.getInt("matrix-mul-npart", -1);
        if(totalSize<0 || partSize<0 || npart<0){
                System.out.println("Error in setup of MyMapper.");
                System.exit(1);
        }
    }

    public void map(LongWritable key, Text value, Context context
                    ) throws IOException, InterruptedException {
        String line=value.toString();
        String[] strs=line.split(" ");
        if(strs.length!=totalSize+1){
                System.out.println("Error in map of Mapper.");
                System.out.println(strs.length+"___"+totalSize);
                System.out.println("line is: "+line);
                System.exit(1);
        }
        int linenum=Integer.parseInt(strs[0]);
        int[] numbers=new int[totalSize];
        for(int i=0;i<totalSize;i++)
                numbers[i]=Integer.parseInt(strs[i+1]);
        int part_hor=linenum/partSize; //horizontal partitioned id
        int prev_part_ver=-1;
        String msg=null;
        for(int i=0;i<totalSize;i++){
                int part_ver=i/partSize; //vertical partition number
                if(part_ver!=prev_part_ver){
                        if(msg!=null){
                                int baselinenum = part_hor * partSize;
                                int old=part_ver;
                                part_ver=prev_part_ver;
                                if(isLeft()){
                                        String toSend="l:"+(linenum - baselinenum)+":"+part_ver+"#"+msg;
                                        System.out.println("left "+linenum+","+part_ver+" "+msg);
                                        for(int k=0;k<npart;k++){
                                                int dest=part_hor * npart + k;
                                                context.write(new IntWritable(dest), new Text(toSend));
                                        }
                                }else{
                                        String toSend="r:"+(linenum - baselinenum)+":"+part_hor+"#"+msg;
                                        System.out.println("right "+part_ver+":"+linenum+" "+msg);
                                        for(int k=0;k<npart;k++){
                                                int dest=k * npart + part_ver;
                                                context.write(new IntWritable(dest), new Text(toSend));
                                        }
                                }
                                part_ver=old;
                        }
                        msg=null;
                        prev_part_ver=part_ver;
                }
                if(msg==null)
                        msg=""+strs[i+1];
                else
                        msg+=" "+strs[i+1];
        }
        if(msg!=null){ //almost the same code 
                int part_ver=npart-1;
                int baselinenum = part_hor * partSize;
                if(isLeft()){
                        String toSend="l:"+(linenum - baselinenum)+":"+part_ver+"#"+msg;
                        System.out.println("left "+linenum+","+part_ver+" "+msg);
                        for(int k=0;k<npart;k++){
                                int dest=part_hor * npart + k;
                                context.write(new IntWritable(dest), new Text(toSend));
                        }
                }else{
                        String toSend="r:"+(linenum - baselinenum)+":"+part_hor+"#"+msg;
                        System.out.println("right "+part_ver+":"+linenum+" "+msg);
                        for(int k=0;k<npart;k++){
                                int dest=k * npart + part_ver; //has to be the last part
                                context.write(new IntWritable(dest), new Text(toSend));
                        }
                }
        }
    }
  }