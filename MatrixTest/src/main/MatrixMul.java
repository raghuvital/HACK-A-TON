package main;

import java.io.IOException;
import java.util.StringTokenizer;

import mapper.MatrixMapper;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

import reducer.MatrixReducer;

public class MatrixMul {

  public static void main(String[] args) throws Exception {
    Configuration configuration = new Configuration(); //
    if (args.length != 4) {
        System.err.println("Usage: MatrixMul input-dir output-dir total-size part-size");
        System.exit(2);
    }
    int totalsize=Integer.parseInt(args[2]); //Reading the dimensionality (Ex: 4*4, 8*8)
    int partsize=Integer.parseInt(args[3]);  //Mapper dividing the given dimensionality matrix into sub module(ex: 2*2)
    if(totalsize==0 || partsize==0 || partsize>totalsize){
        System.out.println("Invalid total-size or part-size");
        System.exit(1);
    }
    configuration.setInt("matrix-mul-totalsize", totalsize); //the matrix is 'totalsize' by 'totalsize'
    configuration.setInt("matrix-mul-partsize", partsize); //every block is 'partsize' by 'partsize'
    int npart=totalsize/partsize;
    if(npart*partsize<totalsize)
        npart++;
    configuration.setInt("matrix-mul-npart", npart); //number of parts on one dimension
    Job job = new Job(configuration, "matrix-mul"); //Intializing the job schedular
    job.setJarByClass(MatrixMul.class);
    job.setMapperClass(MatrixMapper.class);//Setting up the Mapper class
    job.setReducerClass(MatrixReducer.class);// Setting up the Reducer class
    job.setNumReduceTasks(npart*npart);//Setting the number of redcuer workers

    job.setOutputKeyClass(IntWritable.class); //Key of the intermediate level code
    job.setOutputValueClass(Text.class);// value of the key in the intermediate level

    //FileInputFormat.addInputPath(job, new Path(args[0]));
    TextInputFormat.addInputPath(job, new Path(args[0])); //need to read a complete line from input folder specified
    FileOutputFormat.setOutputPath(job, new Path(args[1]));// setting up the output file directory
    job.waitForCompletion(true) ;
  }
}