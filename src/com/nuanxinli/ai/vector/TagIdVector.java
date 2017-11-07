package com.nuanxinli.ai.vector;

import org.apache.log4j.Logger;

/**
 * 含有id和tag属性的稀疏向量，以便在处理向量集合的过程中，标识每个向量，并给它打标签
 * a sparse vector which has id and tag properties, to easily identify it from a collection
 * @id 长整型唯一标识
 * @tag 标签字符串
 * @author 宋辉(Song Hui)
 *
 */
public class TagIdVector extends SparseVector {
	public String tag;
	public Long id;
	
	private static Logger logger = Logger.getLogger(TagIdVector.class);

	/**
	 * 用维度名称初始化向量,id和tag都为空
	 * constructor with 1 parameters
	 * @param divNames 维度名称 - division names
	 */
	public TagIdVector(String[] divNames) {
		super(divNames);
	}

	/**
	 * 用id,维度名称,维度值来初始化向量
	 * constructor with 3 parameters
	 * @param id 
	 * @param divNames 维度名称 - division names
	 * @param divValues 维度值 - division values
	 */
	public TagIdVector(Long id, String[] divNames, float[] divValues) {
		super(divNames, divValues);
		this.id = id;
	}

	/**
	 * 把一组向量，按照所属中心向量打标签。该向量标签的内容，将等于所属中心的标签
	 * 这里判定一个向量属于哪个中心的依据是：与那个中心的点积最大（相同维度多，而且相同维度中的数值也大）
	 * giving a set of vectors, tag each one by centers. 
	 * for one vector, tag it with a center's tag, when their dot product is maximum in all centers.
	 * @param vectors 需要打标签的一组向量 - a set of vectors
	 * @param centers 给定的中心向量 - centers for tagging
	 * @param invalidValue 无效值，当一个向量和所有中心的点积都不超过这个值的时候，就不再依据这个结果，而是简单把改向量标注为属于第一个中心 
	 * - when dot products of the vector and all centers are less than this value, tag it with first center's tag(as the default tag) 
	 * @return
	 */
	public static void tag(TagIdVector[] vectors, TagIdVector[] centers, Float invalidValue) {
		
		for (int i=0; i<vectors.length;i++){
			TagIdVector vector = vectors[i];
			int index = vector.maxDotProduction(centers);
			if (invalidValue!=null){
				float production = vector.dotProduct(centers[index]);
				if (production<invalidValue){
					logger.debug("向量和最近的中心点积为"+production+"，太小放弃，选用缺省值。"+vector);
					index = 0;	//以第一个中心作为缺省值
				}
			}
			vector.tag = centers[index].tag;
			logger.info("为第"+(i)+"个向量打标签:"+index);
		}
	}
	
	/**
	 * 序列化方法。实现方式是在父类序列化内容的前面，加上id和tag的内容
	 * serialized this vector, just add id and tag properties before super string
	 */
	@Override
	public String toString()
	{
		return "id="+id+", tag="+tag+super.toString();
	}
}
