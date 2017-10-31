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

	/**
	 * 双值计算器回调方法,用于在合并向量的时候, 让外部调用指定如何合并
	 * @author Administrator
	 *
	 */
	@FunctionalInterface
	interface DualValueComputer {
		float compute(Float coordValue1, Float coordValue2);
	}
	
	/**
	 * 用一个map来保存向量各个维度的值(仅不为0的维度才保存)
	 */
	private Map<String, Float> coordMap = new HashMap<>();
	//长度的平方(这是一个缓存, 由计算方法squareOfLength生成,一旦生成就记录下来,以后不再计算了, 除非向量改变)
	private Float lengthSquareCache;

	private static Logger logger = Logger.getLogger(SparseVector.class);

	public SparseVector(String[] texts, float[] weights)
	{
		for (int i=0; i<texts.length; i++){
			String text= texts[i];
			float weight = weights[i];
			coordMap.put(text, weight);
		}
	}
	
	public SparseVector(String[] texts)
	{
		for (String text : texts){
			coordMap.put(text, 1f);
		}
	}
	
	public SparseVector() {
		
	}
	
	public void setCoord(String coordName, float coordValue)
	{
		coordMap.put(coordName, coordValue);
		if (lengthSquareCache!=null){
			squareOfLength(true);
		}
		
	}

	public Float getCoordValue(String coordName)
	{
		return coordMap.get(coordName);
	}
	
	public SparseVector plus(SparseVector vector)
	{
		return this.mergeVector(vector, (value1,value2)->value1+value2);
	}
	
	public void plusSelf(SparseVector vector)
	{
		this.mergeVectorSelf(vector, (value1,value2)->value1+value2);
	}
	
	public SparseVector minus(SparseVector vector)
	{
		return this.mergeVector(vector, (value1,value2)->value1-value2);
	}
	
	public void minusSelf(SparseVector vector)
	{
		this.mergeVectorSelf(vector, (value1,value2)->value1-value2);
	}
	
	public SparseVector multiply(float factor)
	{
		SparseVector product = new SparseVector();
		for (Map.Entry<String,Float> entry : coordMap.entrySet()){
			product.setCoord(entry.getKey(), entry.getValue()*factor);
		}
		return product;
	}
	
	public void multiplySelf(float factor)
	{
		for (Map.Entry<String,Float> entry : coordMap.entrySet()){
			this.setCoord(entry.getKey(), entry.getValue()*factor);
		}
	}
	
	public SparseVector divide(float divisor)
	{
		SparseVector quotient = new SparseVector();
		for (Map.Entry<String,Float> entry : coordMap.entrySet()){
			quotient.setCoord(entry.getKey(), entry.getValue()/divisor);
		}
		return quotient;
	}
	
	public void divideSelf(float divisor)
	{
		for (Map.Entry<String,Float> entry : coordMap.entrySet()){
			this.setCoord(entry.getKey(), entry.getValue()/divisor);
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
	 * @param computeFunc 合并算法
	 */
	private SparseVector mergeVector(SparseVector vector, DualValueComputer computeFunc){
		SparseVector newVector = new SparseVector();
		newVector.mergeVectorSelf(this, computeFunc);
		newVector.mergeVectorSelf(vector, computeFunc);
		return newVector;
	}
	
	/**
	 * 把指定向量合并到当前向量
	 * @param vector 指定向量
	 * @param computeFunc 合并算法
	 */
	private void mergeVectorSelf(SparseVector vector, DualValueComputer computeFunc){
		for (Map.Entry<String,Float> entry : vector.coordMap.entrySet()){
			String coordName = entry.getKey();
			Float coordValue = entry.getValue();
			Float originValue = this.getCoordValue(coordName);
			this.setCoord(coordName, (originValue==null)? coordValue: computeFunc.compute(originValue,coordValue));
		}
	}
	
	/**
	 * 把向量集合合并为一个新的向量
	 * @param vectors 向量集合
	 * @param computeFunc 合并算法
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
	 * 计算两个向量的点积
	 * @param vector2
	 * @return
	 */
	public float dotProduct(SparseVector vector2)
	{
		if (vector2==null){
			return 0;
		}
		
		float product = 0;
		for (Map.Entry<String,Float> entry : coordMap.entrySet()){
			String coordName = entry.getKey();
			Float coordValue = entry.getValue();
			Float coordValue2 = vector2.getCoordValue(coordName);
			product += (coordValue2==null ? 0 : coordValue*coordValue2); 
		}
		return product;
	}
	
	/**
	 * 求指定向量与本向量的距离平方, 其实也就是各个维度坐标差的平方和
	 * 因为求距离的话,还要把最后的平方和再开方一次, 如果只是用于比较大小, 得到距离平方就够了
	 * @param vector2
	 * @return
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
	 * @return
	 */
	public float squareOfLength(boolean updateCache)
	{
		if (!updateCache && lengthSquareCache!=null){
			return lengthSquareCache.floatValue();
		}
		
		float sum=0;
		for (float value:coordMap.values()){
			sum += value*value;
		}
		lengthSquareCache = sum;
		return sum;
	}
	
	/**
	 * 余弦相似性
	 * 表达了两个向量夹角的大小
	 * @return
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
	 * @param vectors
	 * @return 最接近向量(点)的索引 (在vectors中的index)
	 */
	public int nearest(SparseVector[] vectors) {
		float[] distances = new float[vectors.length];
		
		for (int i=0; i<vectors.length;i++){
			distances[i] = squareOfDistance(vectors[i]);
		}
		return minIndex(distances);
	}
	
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
	 * 与cloest的差别在于, cloest求向量夹角最小，此时有相同维度的贡献不够突出
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
	
	public String toString()
	{
		return coordMap.entrySet().toString();
	}
	
	public Entry<String,Float>[] topDivisions(int count)
	{
		@SuppressWarnings("unchecked")
		Entry<String,Float>[] set = coordMap.entrySet().stream().sorted((Entry<String,Float> w1, Entry<String,Float> w2)->Float.compare(w2.getValue(),w1.getValue())).toArray(Entry[]::new);
		if (count>=0 && count<set.length){
			return Arrays.copyOfRange(set,0, count);
		}else {
			return set;
		}
	}
	
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
	 * @param vectors
	 * @param centers
	 * @return
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
