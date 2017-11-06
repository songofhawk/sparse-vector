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
 * @author Administrator
 * 宋辉
 *
 */
public class SparseVector {

	// 双值计算器回调方法,用于在合并向量的时候, 让外部调用指定如何合并
	@FunctionalInterface
	public interface DualValueComputer {
		float compute(Float coordValue1, Float coordValue2);
	}
	
	//用一个map来保存向量各个维度的值(仅不为0的维度才保存)
	private Map<String, Float> divMap = new HashMap<>();
	//长度的平方(这是一个缓存, 由计算方法squareOfLength生成,一旦生成就记录下来,以后不再计算了, 除非向量改变)
	private Float lengthSquareCache;

	//日志记录
	private static Logger logger = Logger.getLogger(SparseVector.class);

	/**
	 * 初始化向量
	 * @param divNames 字符数组存储的维度名称
	 * @param divValues 浮点数组存储的维度值
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
	 * @param divNames 字符数组存储的维度名称
	 * 所有的穿入名称的维度值被初始化为1
	 */
	public SparseVector(String[] divNames)
	{
		for (String text : divNames){
			divMap.put(text, 1f);
		}
	}
	
	/**
	 * 初始化向量
	 * 缺省不存在任何维度(相当于在任意维度上的值为0)
	 */
	public SparseVector() {
		
	}
	
	/**
	 * 设置某个特定维度的值
	 * @param divName 维度名称
	 * @param divValue 维度值
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
	 * @param coordName 维度名称
	 * @return 维度值
	 */
	public Float getCoordValue(String coordName)
	{
		return divMap.get(coordName);
	}
	
	/**
	 * 本向量与另一个向量相加，返回新向量
	 * @param vector 加向量
	 * @return 两者之和组成的新向量
	 */
	public SparseVector plus(SparseVector vector)
	{
		return this.mergeVector(vector, (value1,value2)->value1+value2);
	}
	
	/**
	 * 本向量与另一个向量相加，并把结果保存到自身
	 * @param vector 加向量
	 */
	public void plusSelf(SparseVector vector)
	{
		this.mergeVectorSelf(vector, (value1,value2)->value1+value2);
	}
	
	/**
	 * 本向量与另一个向量相减，返回新向量
	 * @param vector 减向量
	 * @return 两者之差组成的新向量
	 */
	public SparseVector minus(SparseVector vector)
	{
		return this.mergeVector(vector, (value1,value2)->value1-value2);
	}
	
	/**
	 * 本向量与另一个向量相减，结果保存到自身
	 * @param vector 减向量
	 */
	public void minusSelf(SparseVector vector)
	{
		this.mergeVectorSelf(vector, (value1,value2)->value1-value2);
	}
	
	/**
	 * 本向量乘以一个常数，返回新向量
	 * @param factor 乘数
	 * @return 两者之积组成的新向量
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
	 * @param factor 乘数
	 */
	public void multiplySelf(float factor)
	{
		for (Map.Entry<String,Float> entry : divMap.entrySet()){
			this.setDiv(entry.getKey(), entry.getValue()*factor);
		}
	}
	
	/**
	 * 本向量除以一个常数，返回新向量
	 * @param factor 除数
	 * @return 两者之商组成的新向量
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
	 * @param factor 除数
	 */
	public void divideSelf(float divisor)
	{
		for (Map.Entry<String,Float> entry : divMap.entrySet()){
			this.setDiv(entry.getKey(), entry.getValue()/divisor);
		}
	}

	/**
	 * 获取指定向量(点)集合的图心(几何中心)
	 * @param vectors 指定向量集合
	 * @return 几何中心向量
	 */
	public static SparseVector getCentroid(Collection<SparseVector> vectors)
	{
		SparseVector centroid = mergeVectors(vectors, (value1, value2)->value1+value2);
		centroid.divideSelf(vectors.size());
		return centroid;
	}
	
	/**
	 * 把当前向量和指定向量合并为一个新向量
	 * @param vector 指定向量
	 * @param computeFunc 合并算法，用于表达两个具体的向量维度合并时应该怎样计算
	 */
	public SparseVector mergeVector(SparseVector vector, DualValueComputer computeFunc){
		SparseVector newVector = new SparseVector();
		newVector.mergeVectorSelf(this, computeFunc);
		newVector.mergeVectorSelf(vector, computeFunc);
		return newVector;
	}
	
	/**
	 * 把指定向量合并到当前向量
	 * @param vector 指定向量
	 * @param computeFunc 合并算法，用于表达两个具体的向量维度合并时应该怎样计算
	 */
	private void mergeVectorSelf(SparseVector vector, DualValueComputer computeFunc){
		for (Map.Entry<String,Float> entry : vector.divMap.entrySet()){
			String coordName = entry.getKey();
			Float coordValue = entry.getValue();
			Float originValue = this.getCoordValue(coordName);
			this.setDiv(coordName, (originValue==null)? coordValue: computeFunc.compute(originValue,coordValue));
		}
	}
	
	/**
	 * 把一组向量集合合并为一个新的向量
	 * @param vectors 向量集合
	 * @param computeFunc 合并算法，用于表达两个具体的向量维度合并时应该怎样计算
	 * @return
	 */
	private static SparseVector mergeVectors(Collection<SparseVector> vectors, DualValueComputer computeFunc)
	{
		SparseVector newVector = new SparseVector();
		for (SparseVector vector : vectors){
			newVector.mergeVectorSelf(vector, computeFunc);
		}
		return newVector;
	}
	
	/**
	 * 计算本向量与指定向量的点积(标量积)
	 * @param vector 指定向量
	 * @return 点积值
	 */
	public float dotProduct(SparseVector vector)
	{
		if (vector==null){
			return 0;
		}
		
		float product = 0;
		for (Map.Entry<String,Float> entry : divMap.entrySet()){
			String coordName = entry.getKey();
			Float coordValue = entry.getValue();
			Float coordValue2 = vector.getCoordValue(coordName);
			product += (coordValue2==null ? 0 : coordValue*coordValue2); 
		}
		return product;
	}

	/**
	 * 求指定向量与本向量的距离平方, 其实也就是各个维度坐标差的平方和
	 * 因为求距离的话,还要把最后的平方和再开方一次, 如果只是用于比较大小, 得到距离平方就够了
	 * @param vector2
	 * @return 距离的平方
	 */
	public float squareOfDistance(SparseVector vector2)
	{
		if (vector2==null){
			return this.squareOfLength(false);
		}
		SparseVector newVector = minus(vector2);
		return newVector.squareOfLength(false);
	}
	
	/**
	 * 向量长度(模)的平方
	 * @param updateCache 是否强制更新缓存
	 * 如果这个参数为true，表示无论以前是否计算过，强制根据各个维度的值重新计算长度，并写入缓存
	 * 如果这个参数为false，那么会根据是否计算过这个值，来决定从缓存中获取长度，还是根据维度的值来计算长度
	 * @return 长度的平方
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
	 * 余弦相似性
	 * 表达了两个向量夹角的大小
	 * @return 夹角的余璇值
	 */
	public float cosineSimilarity(SparseVector vector2)
	{
		if (vector2==null){
			throw new RuntimeException("0向量无法计算余弦相似性");
		}
		return dotProduct(vector2) / (squareOfLength(false) * vector2.squareOfLength(false));
	}
	
	
	
	/**
	 * 求与当前向量(点)距离最近的向量(点)
	 * @param vectors 一组要查找的向量
	 * @return 最接近向量(点)的索引 (在vectors中的index)
	 */
	public int nearest(SparseVector[] vectors) {
		float[] distances = new float[vectors.length];
		
		for (int i=0; i<vectors.length;i++){
			distances[i] = squareOfDistance(vectors[i]);
		}
		return minIndex(distances);
	}
	
	//数组中最小值的索引
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
	 * 求与当前向量(点)夹角最小的向量(点)
	 * 与nearest的差别在于, nearest求向量差最小,而本函数求向量夹角最小----也就是余弦相似性最大
	 * @param vectors 要一一求夹角的向量组(如果其中有null值,直接忽略, 不会当做0向量处理)
	 * @return 最靠近向量(点)的索引 (在vectors中的index)
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
	 * 求与当前向量(点)点积最大向量(点)
	 * 与cloest的差别在于, cloest求向量夹角最小，此时两个向量有相同维度的贡献不够突出，而计算点积的时候，只要两个向量在相同维度上有值，就一定能获得一个比较大的乘积
	 * @param vectors 要一一求点积的向量组(如果其中有null值,直接忽略, 不会当做0向量处理)
	 * @return 最大点积(点)的索引 (在vectors中的index)
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
	 */
	public String toString()
	{
		return divMap.entrySet().toString();
	}
	
	/**
	 * 寻找数值最大的几个维度值
	 * @param count
	 * @return
	 */
	public Entry<String,Float>[] topDivisions(int count)
	{
		@SuppressWarnings("unchecked")
		Entry<String,Float>[] set = divMap.entrySet().stream().sorted((Entry<String,Float> w1, Entry<String,Float> w2)->Float.compare(w2.getValue(),w1.getValue())).toArray(Entry[]::new);
		if (count>=0 && count<set.length){
			return Arrays.copyOfRange(set,0, count);
		}else {
			return set;
		}
	}
	
	/**
	 * 寻找数值最大的几个维度的名称
	 * @param count
	 * @return
	 */
	public String topDivString(int count)
	{
		Entry<String,Float>[] topEnties = topDivisions(count);
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
	 * @param vectors 待分类的向量组
	 * @param centers 给定的向量中心
	 * @return 分好类的向量组，外围List对应centers（分类中心的个数)
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
