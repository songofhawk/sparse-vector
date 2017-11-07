# sparse-vector

这是一个非常简单的稀疏向量运算库，实现了向量的模、加、减、数值乘、数值除和点积运算，并且加入了欧氏距离、余弦相似性和杰卡得相似性的比较。

这个项目来源于暖心理内部的自然语言标签系统，由于项目的数据量小，需求简单，如果选用完整的机器学习平台，或者科学计算库，有点杀鸡用牛刀的感觉，所以做了这个小库。

稀疏向量的运算规则本来跟普通向量相同，但是维度数量大（可能成千上万乃至上百万个），而具体到每一个向量，绝大多数维度的值又都是零，这在自然语言处理时非常常见。如果采用数组或列表保存，过于浪费计算时间和空间，所以在本项目中采用了String作为key的HashMap来存储（普通的HashMap,并不是线程安全的）。

A simple sparse vector library, implements the following operation: norm, plus, minus, numerical multiply, numerical divide and dot product. In addition, this lib offers Euclidean distance, cosine similarity and Jaccard similarity comparison.

The project is derived from a natural language tagging system in [NuanXinLi http://www.nuanxinli.com] (a psychological service platform based on mobile Internet). When data size is small, requirement is simple, a machine learning platform or full scientific computing lib seems not necessary, therefor this lib borning.

Sparse vectors usually has large amount of divisions, from thousands to millions, but most of its division has 0 value. If stores these divisions by an array of a list, space and computing resource using will be huge. In this project, we use a map to store all none-zero divisions, which is a normal HashMap, not thread-safe.

# runtime
Jre 8

# dependence configuration
maven
