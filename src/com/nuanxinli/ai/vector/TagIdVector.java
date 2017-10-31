package com.nuanxinli.ai.vector;

import org.apache.log4j.Logger;

public class TagIdVector extends SparseVector {
	public String tag;
	public Long id;
	
	private static Logger logger = Logger.getLogger(TagIdVector.class);

	public TagIdVector(String[] keys) {
		super(keys);
	}


	public TagIdVector(Long id, String[] keys, float[] weights) {
		super(keys, weights);
		this.id = id;
	}

	/**
	 * 把一组向量，按照给定的几个中心向量打标签
	 * @param vectors
	 * @param centers
	 * @return
	 */
	public static void tag(TagIdVector[] vectors, TagIdVector[] centers, Float minValue) {
		
		for (int i=0; i<vectors.length;i++){
			TagIdVector vector = vectors[i];
			int index = vector.maxDotProduction(centers);
			if (minValue!=null){
				float production = vector.dotProduct(centers[index]);
				if (production<minValue){
					logger.debug("向量和最近的中心点积为"+production+"，太小放弃，选用缺省值。"+vector);
					index = 0;	//以第一个中心作为缺省值
				}
			}
			vector.tag = centers[index].tag;
			logger.info("为第"+(i)+"个向量打标签:"+index);
		}
	}
	
	@Override
	public String toString()
	{
		return "id="+id+", tag="+tag+super.toString();
	}
}
