package com.nuanxinli.ai.vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiFunction;

import org.apache.log4j.Logger;

/**
 * 稀疏向量
 * Sparse Vector
 * @author 宋辉(Song Hui)
 * 
 */
public class SparseVector {

	// 双值计算器回调方法,用于在合并向量的时候, 让外部调用指定如何合并
	//a callback function, to calculate two values when mergeing vectors
	@FunctionalInterface
	public interface DualValueComputer {
		float calculate(Float divValue1, Float divValue2);
	}
	
	//用一个map来保存向量各个维度的值(仅不为0的维度才保存)
	//a map to store values for all divisions, only none-zero value will be stored
	private Map<String, Float> divMap = new HashMap<>();
	//长度的平方(这是一个缓存, 由计算方法squareOfLength生成,一旦生成就记录下来,以后不再计算了, 除非向量改变)
	//the square of length on this vector, which is a cache value 
	private Float lengthSquareCache;
	//所有维度值之和(这是一个缓存, 由计算方法sum生成,一旦生成就记录下来,以后不再计算了, 除非向量改变)
	//the sum on values of all divisions 
	private Float sumCache;

	//日志记录
	//for logging
	private static Logger logger = Logger.getLogger(SparseVector.class);

	/**
	 * 初始化向量
	 * constructor with 2 parameters
	 * @param divNames 字符数组存储的维度名称 - division names in string array 
	 * @param divValues 浮点数组存储的维度值 - division values in float array
	 */
	public SparseVector(String[] divNames, float[] divValues)
	{
		for (int i=0; i<divNames.length; i++){
			String text= divNames[i];
			float value = divValues[i];
			divMap.put(text, value);
		}
	}
	
	/**
	 * 初始化向量
	 * constructor with 1 parameter
	 * @param divNames 字符数组存储的维度名称 - division names in string array 
	 * 所有的传入名称的维度值被初始化为1
	 * all division values will be initialized to 1 in divNames
	 */
	public SparseVector(String[] divNames)
	{
		for (String text : divNames){
			divMap.put(text, 1f);
		}
	}
	
	/**
	 * 初始化向量
	 * constructor with no parameter
	 * 缺省不存在任何维度(相当于在任意维度上的值为0)
	 */
	public SparseVector() {
		
	}
	
	/**
	 * 设置某个特定维度的值
	 * set value of specified division 
	 * @param divName 维度名称 - division name
	 * @param divValue 维度值 - division value
	 */
	public void setDiv(String divName, float divValue)
	{
		divMap.put(divName, divValue);
		if (lengthSquareCache!=null){
			squareOfLength(true);
		}
	}

	/**
	 * 获取某个特定维度的值 
	 * get value of specified division
	 * @param divName 维度名称 - division name 
	 * @return 维度值 - division value
	 */
	public Float getCoordValue(String divName)
	{
		return divMap.get(divName);
	}
	
	/**
	 * 本向量与另一个向量相加，返回新向量
	 * return a new vector, which equals this vector plus another one
	 * as the count of divisions is not certain, any two vectors can be calculated by this function
	 * @param vector 加向量 - another vector
	 * @return 两者之和组成的新向量 - sum vector
	 */
	public SparseVector plus(SparseVector vector)
	{
		return this.mergeVector(vector, (value1,value2)->value1+value2);
	}
	
	/**
	 * 本向量与另一个向量相加，并把结果保存到自身
	 * plus another vector to this vector, this vector will equal the sum vector after function called.
	 * as the count of divisions is not certain, any two vectors can be calculated by this function
	 * @param vector 加向量 - another vector
	 */
	public void plusSelf(SparseVector vector)
	{
		this.mergeVectorSelf(vector, (value1,value2)->value1+value2);
	}
	
	/**
	 * 本向量与另一个向量相减，返回新向量
	 * return a new vector, which equals this vector minus another one
	 * as the count of divisions is not certain, any two vectors can be calculated by this function
	 * @param vector 减向量 - another vector
	 * @return 两者之差组成的新向量 - difference vector
	 */
	public SparseVector minus(SparseVector vector)
	{
		return this.mergeVector(vector, (value1,value2)->value1-value2);
	}
	
	/**
	 * 本向量与另一个向量相减，结果保存到自身
	 * this vector minus another one, this vector will equal the difference vector after function called.
	 * as the count of divisions is not certain, any two vectors can be calculated by this function
	 * @param vector 减向量 - another vector
	 */
	public void minusSelf(SparseVector vector)
	{
		this.mergeVectorSelf(vector, (value1,value2)->value1-value2);
	}
	
	/**
	 * 本向量乘以一个常数，返回新向量
	 * return a new vector, which equals this vector multiply a constant
	 * @param factor 乘数 - the constant
	 * @return 两者之积组成的新向量 - the result vector
	 */
	public SparseVector multiply(float factor)
	{
		SparseVector product = new SparseVector();
		for (Map.Entry<String,Float> entry : divMap.entrySet()){
			product.setDiv(entry.getKey(), entry.getValue()*factor);
		}
		return product;
	}
	
	/**
	 * 本向量乘以一个常数，结果保存到自身
	 * this vector multiply a constant, this vector will equal the product vector after function called.
	 * @param factor 乘数 - the constant
	 */
	public void multiplySelf(float factor)
	{
		for (Map.Entry<String,Float> entry : divMap.entrySet()){
			this.setDiv(entry.getKey(), entry.getValue()*factor);
		}
	}
	
	/**
	 * 本向量除以一个常数，返回新向量
	 * return a new vector, which equals this vector divide a constant
	 * @param divisor 除数 - divisor
	 * @return 两者之商组成的新向量 - the quotient vector
	 */
	public SparseVector divide(float divisor)
	{
		SparseVector quotient = new SparseVector();
		for (Map.Entry<String,Float> entry : divMap.entrySet()){
			quotient.setDiv(entry.getKey(), entry.getValue()/divisor);
		}
		return quotient;
	}
	
	/**
	 * 本向量除以一个常数，结果保存到自身
	 * this vector divide by a constant, this vector will equal the quotient vector after function called.
	 * @param factor 除数 - divisor
	 */
	public void divideSelf(float divisor)
	{
		for (Map.Entry<String,Float> entry : divMap.entrySet()){
			this.setDiv(entry.getKey(), entry.getValue()/divisor);
		}
	}

	/**
	 * 获取指定向量(点)集合的图心(几何中心)
	 * Get the centroid of a collection of vectors, also called as geometric center
	 * @param vectors 指定向量集合 - the collection of vectors
	 * @return 几何中心向量 - the centroid
	 */
	public static SparseVector getCentroid(Collection<SparseVector> vectors)
	{
		SparseVector centroid = mergeVectors(vectors, (value1, value2)->value1+value2);
		centroid.divideSelf(vectors.size());
		return centroid;
	}
	
	/**
	 * 获取指定向量(点)集合的近似图心(几何中心)
	 * Get the centroid of a collection of vectors, also called as geometric center
	 * 这是一个重载方法,增加了minRatio参数, 用于忽略哪些占比很小的维度：
	 * 方法会在合并完成后，计算一下每一个维度值占总值的比例，如果低于给定的minRatio值，就忽略这个维度，以便节省空间，提高效率
	 * @param vectors 指定向量集合 - the collection of vectors
	 * @return 几何中心向量 - the centroid
	 * @param minRatio 最小比例值，小于这个比例的维度将被删除 - the minimal ratio, all divisions its value ratio lower than this value will be deleted
	 */
	public static SparseVector getCentroid(Collection<SparseVector> vectors, Float minRatio)
	{
		SparseVector centroid = mergeVectors(vectors, (value1, value2)->value1+value2, minRatio);
		centroid.divideSelf(vectors.size());
		return centroid;
	}
	  
	/**
	 * 把本向量和指定向量合并为一个新向量； 如果两者有相同的维度，它们的值会合并，合并方式取决于calculateFunc参数；如果是不同的维度，会直接被复制到结果中
	 * merge this vector with another one, and return the result as a new vector.
	 * the result will have union divisions of these 2 vectors. The same divisions will be merged, merging method depends on calculateFunc. 
	 * the different divisions will be copied into result directly.    
	 * @param vector 指定向量 - another vector
	 * @param calculateFunc 合并算法，用于表达两个具体的向量维度合并时应该怎样计算 - merging method, which is a call back function to handle values from 2 vectors
	 */
	public SparseVector mergeVector(SparseVector vector, DualValueComputer calculateFunc){
		SparseVector newVector = new SparseVector();
		newVector.mergeVectorSelf(this, calculateFunc);
		newVector.mergeVectorSelf(vector, calculateFunc);
		return newVector;
	}
	
	/**
	 * 把本向量和指定向量合并为一个新向量； 
	 * 这是一个重载方法,增加了minRatio参数, 用于忽略哪些占比很小的维度：
	 * 方法会在合并完成后，计算一下每一个维度值占总值的比例，如果低于给定的minRatio值，就忽略这个维度，以便节省空间，提高效率
	 * merge this vector with another one, and return the result as a new vector.
	 * the result will have union divisions of these 2 vectors. The same divisions will be merged, merging method depends on calculateFunc. 
	 * the different divisions will be copied into result directly.    
	 * @param vector 指定向量 - another vector
	 * @param calculateFunc 合并算法，用于表达两个具体的向量维度合并时应该怎样计算 - merging method, which is a call back function to handle values from 2 vectors
	 * @param minRatio 最小比例值，小于这个比例的维度将被删除 - the minimal ratio, all divisions its value ratio lower than this value will be deleted
	 */
	public SparseVector mergeVector(SparseVector vector, DualValueComputer calculateFunc, Float minRatio){
		SparseVector newVector = new SparseVector();
		newVector.mergeVectorSelf(this, calculateFunc);
		newVector.mergeVectorSelf(vector, calculateFunc, minRatio);
		return newVector;
	}
	
	/**
	 * 把指定向量合并到本向量； 如果两者有相同的维度，它们的值会合并，合并方式取决于calculateFunc参数；如果是不同的维度，会直接被复制到本向量中
	 * merge another vector into this one.
	 * the result will have union divisions of these 2 vectors. The same divisions will be merged, merging method depends on calculateFunc. 
	 * the different divisions will be copied into this one directly.    
	 * @param vector 指定向量 - another vector
	 * @param calculateFunc 合并算法，用于表达两个具体的向量维度合并时应该怎样计算 - merging method, which is a call back function to handle values from 2 vectors
	 */
	private void mergeVectorSelf(SparseVector vector, DualValueComputer calculateFunc){
		for (Map.Entry<String,Float> entry : vector.divMap.entrySet()){
			String divName = entry.getKey();
			Float divValue = entry.getValue();
			Float originValue = this.getCoordValue(divName);
			this.setDiv(divName, (originValue==null)? divValue: calculateFunc.calculate(originValue,divValue));
		}
	}
	
	/**
	 * 把指定向量合并到本向量； 
	 * 这是一个重载方法,增加了minRatio参数, 用于忽略哪些占比很小的维度：
	 * 方法会在合并完成后，计算一下每一个维度值占总值的比例，如果低于给定的minRatio值，就忽略这个维度，以便节省空间，提高效率
	 * merge another vector into this one.
	 * @param vector 指定向量 - another vector
	 * @param calculateFunc 合并算法，用于表达两个具体的向量维度合并时应该怎样计算 - merging method, which is a call back function to handle values from 2 vectors
	 * @param minRatio 最小比例值，小于这个比例的维度将被删除 - the minimal ratio, all divisions its value ratio lower than this value will be deleted
	 */
	private void mergeVectorSelf(SparseVector vector, DualValueComputer calculateFunc, Float minRatio){
		this.mergeVectorSelf(vector, calculateFunc);
		cleanSmallDiv(minRatio);
	}

	//计算一下每一个维度值占总值的比例，删除低于给定minRatio值的维度
	private void cleanSmallDiv(Float minRatio) {
		float totalValue = this.sum(true);
		for ( Entry<String, Float> entry:divMap.entrySet()){
			Float value = entry.getValue();
			if (value/totalValue < minRatio){
				divMap.remove(entry.getKey());
			}
		}
	}
	
	/**
	 * 把一组向量集合合并为一个新的向量
	 * merge a set of vectors to one vector.
	 * the result will have union divisions of all vectors in the set. The same divisions will be merged, merging method depends on calculateFunc. 
	 * the different divisions will be copied to the result.    
	 * @param vectors 向量集合
	 * @param calculateFunc 合并算法，用于表达两个具体的向量维度合并时应该怎样计算
	 * @return
	 */
	private static SparseVector mergeVectors(Collection<SparseVector> vectors, DualValueComputer calculateFunc)
	{
		SparseVector newVector = new SparseVector();
		for (SparseVector vector : vectors){
			newVector.mergeVectorSelf(vector, calculateFunc);
		}
		return newVector;
	}
	
	/**
	 * 把一组向量集合合并为一个新的向量
	 * 这是一个重载方法,增加了minRatio参数, 用于忽略哪些占比很小的维度：
	 * 方法会在合并完成后，计算一下每一个维度值占总值的比例，如果低于给定的minRatio值，就忽略这个维度，以便节省空间，提高效率
	 * merge a set of vectors to one vector.
	 * the result will have union divisions of all vectors in the set. The same divisions will be merged, merging method depends on calculateFunc. 
	 * the different divisions will be copied to the result.    
	 * @param vectors 向量集合
	 * @param calculateFunc 合并算法，用于表达两个具体的向量维度合并时应该怎样计算
	 * @param minRatio 最小比例值，小于这个比例的维度将被删除 - the minimal ratio, all divisions its value ratio lower than this value will be deleted
	 * @return
	 */
	private static SparseVector mergeVectors(Collection<SparseVector> vectors, DualValueComputer calculateFunc, Float minRatio)
	{
		SparseVector newVector = new SparseVector();
		for (SparseVector vector : vectors){
			newVector.mergeVectorSelf(vector, calculateFunc, minRatio);
		}
		newVector.cleanSmallDiv(minRatio);
		return newVector;
	}
	
	/**
	 * 计算本向量与指定向量的点积(标量积)
	 * calculate "dot product/scalar product" of this vector and another vector
	 * @param vector 指定向量 - another vector
	 * @return 点积值 - dot product value
	 */
	public float dotProduct(SparseVector vector)
	{
		if (vector==null){
			return 0;
		}
		
		float product = 0;
		for (Map.Entry<String,Float> entry : divMap.entrySet()){
			String divName = entry.getKey();
			Float divValue = entry.getValue();
			Float divValue2 = vector.getCoordValue(divName);
			product += (divValue2==null ? 0 : divValue*divValue2); 
		}
		return product;
	}

	/**
	 * 求指定向量与本向量的距离平方, 其实也就是各个维度坐标差的平方和
	 * 因为求距离的话,还要把最后的平方和再开方一次, 如果只是用于比较大小, 得到距离平方就够了
	 * Get the square of distance between this vector and another one. 
	 * @param vector 指定向量(如果为空,表示0向量,则返回本向量的长度) - another vector(if is null, than return the square of length of this vector)
	 * @return 距离的平方 - the square of distance
	 */
	public float squareOfDistance(SparseVector vector)
	{
		if (vector==null){
			return this.squareOfLength(false);
		}
		SparseVector newVector = minus(vector);
		return newVector.squareOfLength(false);
	}
	
	/**
	 * 求指定向量与本向量的距离
	 * Get the distance between this vector and another one. 
	 * @param  指定向量(如果为空,表示0向量,则返回本向量的长度) - another vector(if is null, then return the length of this vector)
	 * @return 距离 - the distance
	 */
	public float distance(SparseVector vector)
	{
		if (vector==null){
			return this.length(false);
		}
		SparseVector newVector = minus(vector);
		return newVector.length(false);
	}
	
	/**
	 * 向量各个维度值的总和
	 * the sum on values of all divisions
	 * @param updateCache 是否强制更新缓存 - whether force to update cache
	 * 如果这个参数为true，表示无论以前是否计算过，强制根据各个维度的值重新计算长度，并写入缓存
	 * if true, then recalculate the length, and update cache
	 * 如果这个参数为false，那么会根据是否计算过这个值，来决定从缓存中获取长度，还是根据维度的值来计算长度
	 * if false, then get value from cache when existing, otherwise recalculate
	 * @return 维度值的总和 - the sum on values of all divisions
	 */
	public float sum(boolean updateCache)
	{
		if (!updateCache && sumCache!=null){
			return sumCache.floatValue();
		}
		
		float sum=0;
		for (float value:divMap.values()){
			sum += value;
		}
		sumCache = sum;
		return sum;
	}
		
	/**
	 * 向量长度(模)的平方
	 * the square of vector length(norm of vector)
	 * @param updateCache 是否强制更新缓存 - whether force to update cache
	 * 如果这个参数为true，表示无论以前是否计算过，强制根据各个维度的值重新计算长度，并写入缓存
	 * if true, then recalculate the length, and update cache
	 * 如果这个参数为false，那么会根据是否计算过这个值，来决定从缓存中获取长度，还是根据维度的值来计算长度
	 * if false, then get value from cache when existing, otherwise recalculate
	 * @return 长度的平方 - the square of vector length
	 */
	public float squareOfLength(boolean updateCache)
	{
		if (!updateCache && lengthSquareCache!=null){
			return lengthSquareCache.floatValue();
		}
		
		float sum=0;
		for (float value:divMap.values()){
			sum += value*value;
		}
		lengthSquareCache = sum;
		return sum;
	}
	
	/**
	 * 向量长度(模)的平方
	 * the square of vector length(norm of vector)
	 * @return 长度的平方 - the square of vector length
	 */
	public float squareOfLength()
	{
		return squareOfLength(false);
	}
	
	/**
	 * 向量长度(模)
	 * the vector length(norm of vector)
	 * @param updateCache 是否强制更新缓存 - whether force to update cache
	 * 如果这个参数为true，表示无论以前是否计算过，强制根据各个维度的值重新计算长度，并写入缓存
	 * if true, then recalculate the length, and update cache
	 * 如果这个参数为false，那么会根据是否计算过这个值，来决定从缓存中获取长度，还是根据维度的值来计算长度
	 * if false, then get value from cache when existing, otherwise recalculate
	 * @return 长度的平方 - vector length
	 */
	public float length(boolean updateCache)
	{
		return (float) Math.sqrt(squareOfLength(updateCache));
	}
	
	/**
	 * 向量长度(模)
	 * the vector length(norm of vector)
	 * @return 长度的平方 - vector length
	 */
	public float length()
	{
		return length(false);
	}
	
	
	/**
	 * 余弦相似性
	 * 表达了两个向量在方向上的接近程度，或者说夹角的大小
	 * get cosine similarity, which show closing degree in direction
	 * @param vector - another vector
	 * @return 夹角的余弦值，余弦值的范围在[-1,1]之间，值越趋近于1，代表两个向量的方向越接近；越趋近于-1，他们的方向越相反；接近于0，表示两个向量近乎于正交；
	 * cosine similarity [https://en.wikipedia.org/wiki/Cosine_similarity]
	 */
	public float cosineSimilarity(SparseVector vector)
	{
		if (vector==null){
			throw new RuntimeException("0向量无法计算余弦相似性");
		}
		return dotProduct(vector) / (squareOfLength(false) * vector.squareOfLength(false));
	}
	
	/**
	 * 杰卡得相似性
	 * 表达了两个向量维度相似的程度, 等于两个向量维度交集的个数除以并集的个数
	 * get Jaccard similarity, which shows similarity of divisions, equals count of intersection set divides count of union set
	 * @param vector - another vector
	 * @return 维度相似度
	 * Jaccard similarity [https://en.wikipedia.org/wiki/Jaccard_index]
	 */
	public float jaccardSimilarity(SparseVector vector)
	{
		if (vector==null){
			return 0;
		}
		int unionCount = divMap.size()+vector.divMap.size();
		if (unionCount==0){
			return 0;
		}
		
		int intersectCount = 0;
		for (Map.Entry<String,Float> entry : divMap.entrySet()){
			String divName = entry.getKey();
			if (vector.divMap.containsKey(divName)){
				intersectCount++;
			}
		}
		return (float)intersectCount / (float)unionCount;
	}
	
	/**
	 * 求与本向量(点)距离最近的向量(点)
	 * giving a set of vectors, find minimal distance between this vector and one of them, return the index of minimal one 
	 * @param vectors 一组要查找的向量 - a set of vectors
	 * @return 最接近向量(点)的索引 (在vectors中的index) - the index of the minimal one 
	 */
	public int nearest(SparseVector[] vectors) {
		float[] distances = new float[vectors.length];
		
		for (int i=0; i<vectors.length;i++){
			distances[i] = squareOfDistance(vectors[i]);
		}
		return minIndex(distances);
	}
	
	//数组中最小值的索引
	//the index of maximum value in the array 
	private int minIndex(float[] array){
		float minValue = array[0];
		int index = 0;
		for (int i=1; i<array.length;i++){
			float current = array[i];
			if (current<minValue){
				minValue = current;
				index = i;
			}
		}
		return index;
	}

	//数组中最大值的索引
	//the index of maximum value in the array
	private int maxIndex(float[] array){
		float maxValue = array[0];
		int index = 0;
		for (int i=1; i<array.length;i++){
			float current = array[i];
			if (current>maxValue){
				maxValue = current;
				index = i;
			}
		}
		return index;
	}
	
	/**
	 * 求与本向量(点)夹角最小的向量(点)
	 * 与nearest的差别在于, nearest求向量差最小,而本函数求向量夹角最小----也就是余弦相似性最大
	 * giving a set of vectors, find minimal intersection angle between this vector and one of them, return the index of minimal one 
	 * @param vectors 给定的向量组(如果其中有null值,直接忽略, 不会当做0向量处理) - a set of vectors(null item will be ignored) 
	 * @return 最靠近向量(点)的索引 (在vectors中的index) - the index of minimal one 
	 */
	public int cloest(SparseVector[] vectors) {
		float[] similarities = new float[vectors.length];
		
		for (int i=0; i<vectors.length;i++){
			SparseVector vector = vectors[i];
			if (vector==null){
				continue;
			}
			similarities[i] = cosineSimilarity(vector);
		}
		return maxIndex(similarities);
	}
	
	/**
	 * 求与本向量(点)点积最大向量(点)
	 * 与cloest的差别在于, cloest求向量夹角最小，此时两个向量有相同维度的贡献不够突出，而计算点积的时候，只要两个向量在相同维度上有值，就一定能获得一个比较大的乘积
	 * giving a set of vectors, find maximal dot product between this vector and one of them, return the index of maximal one 
	 * @param vectors 要一一求点积的向量组(如果其中有null值,直接忽略, 不会当做0向量处理) - a set of vectors
	 * @return 最大点积(点)的索引 (在vectors中的index) - the index of maximal one 
	 */
	public int maxDotProduction(SparseVector[] vectors) {
		float[] productions = new float[vectors.length];
		
		for (int i=0; i<vectors.length;i++){
			SparseVector vector = vectors[i];
			if (vector==null){
				continue;
			}
			productions[i] = dotProduct(vector);
		}
		return maxIndex(productions);
	}
	
	/**
	 * 把维度名称和维度值序列化
	 * serialized this vector, simply by serializing the map stores divisions
	 */
	public String toString()
	{
		return divMap.entrySet().toString();
	}
	
	/**
	 * 寻找数值最大的几个维度值
	 * descending sort all divisions by value, and return top x of them 
	 * @param x
	 * @return 
	 */
	public Entry<String,Float>[] topDivisions(int x)
	{
		@SuppressWarnings("unchecked")
		Entry<String,Float>[] set = divMap.entrySet().stream().sorted((Entry<String,Float> w1, Entry<String,Float> w2)->Float.compare(w2.getValue(),w1.getValue())).toArray(Entry[]::new);
		if (x>=0 && x<set.length){
			return Arrays.copyOfRange(set,0, x);
		}else {
			return set;
		}
	}
	
	/**
	 * 寻找数值最大的几个维度的名称
	 * descending sort all divisions by value, and return names of top x of them 
	 * @param x
	 * @return
	 */
	public String topDivNames(int x)
	{
		Entry<String,Float>[] topEnties = topDivisions(x);
		StringBuilder builder= new StringBuilder("{");
		for (int i=0; i<topEnties.length; i++){
			Entry<String,Float> entry = topEnties[i];
			//float weight = columnWeights[i];
			builder.append(entry.getKey()).append(":").append(entry.getValue());
			if (i<topEnties.length - 1){
				builder.append(", ");
			}
		}
		builder.append("}");
		return builder.toString();
	}
	
	
	/**
	 * 把一组向量，按照给定的几个中心向量分类
	 * 向量究竟离哪个中心最近，由ruleFunc决定
	 * giving a set of vectors, split them into several groups surrounding given centers, use ruleFunc decide which center is closest to a specified vector
	 * @param vectors 待分类的向量组
	 * a set of vectors
	 * @param centers 给定的向量中心
	 * the centers for grouping
	 * @param ruleFunc 需要传入一个双参数的方法，第一个参数是独立的vector，第二个参数是一组vector，返回一组vectors中，与独立vector关系最近的那条向量的索引
	 * should be a function with 2 arguments, the first is a vector, the second is an array of vectors, and returns the index of closest one in array  
	 * @return 分好类的向量组，外围List对应centers（分类中心的个数)
	 * groups in outer list, and items in one group in inner list
	 */
	public static List<List<SparseVector>> aggregate(SparseVector[] vectors, SparseVector[] centers, BiFunction<SparseVector, SparseVector[],Integer> ruleFunc) {
		
		int count = centers.length;
		List<List<SparseVector>> clusteredVectors = new ArrayList<List<SparseVector>>(count);
		for (int k=0; k<count; k++){
			clusteredVectors.add(null);
		}
		
		int size = vectors.length;
		
		for (int i=0; i<size;i++){
			SparseVector vector = vectors[i];
			int nearestIndex = ruleFunc.apply(vector,centers);
			List<SparseVector> oneClusteredVectors  = clusteredVectors.get(nearestIndex);
			if (oneClusteredVectors==null){
				oneClusteredVectors= new ArrayList<SparseVector>();
				clusteredVectors.set(nearestIndex, oneClusteredVectors);
			}
			oneClusteredVectors.add(vector);
			logger.info("为第"+(i)+"行向量分配所属聚类:"+nearestIndex);
		}
		return clusteredVectors;
	}

}
