matrix-matrix multiplication on Hadoop


A x B = C
constraint: A, B, C must be of the same size

I use this to evaluate the efficiency of Hadoop for matrix multiplication,
so I really don't care to handle non-square matrices.

===Data preparation====
Matrix data must be stored in a file on Hadoop.
Line number must be appended to the beginning of each line.
For example, the following represents a 4x4 matrix:

0 18 20 16 14
1 17 12 11 19
2 10 17 11 19
3 14 17 20 10

Left (A in this example) matrix should be stored in file "left";
Right (B in this example) matrix should be stored in file "right";
I use filenames to distinguish input data.

Place "left" and "right" in the same folder (let's call it "input")

====Run the program====
> hadoop jar matrixmul.jar MatrixMul input output 8 2

results will be placed in "output" folder on HDFS.
8: all matrices are 8x8
2: every partitioned block is of size 2x2

===Read the results===
Given the above sample command, we multiply two 8x8 matrices,
in many 2x2 blocks. So, that the resulted C matrix has 16 blocks.

In the output folder, there will be 16 separate files:
part-r-00000, part-r-00001, ... part-r-00015

Every file stores one block in C. In this example, every block
has 2 rows and 2 columns.

These files are organized in "row"-order.

===Algorithm===
Mappers read input data.
Every reducer processes one block of the resulted matrix.