package com.alibaba.dubbo.remoting;

import com.alibaba.dubbo.common.extension.ExtensionLoader;

public class Cluster$Adaptive implements com.alibaba.dubbo.rpc.cluster.Cluster {
    @Override
    public com.alibaba.dubbo.rpc.Invoker join(com.alibaba.dubbo.rpc.cluster.Directory arg0) throws com.alibaba.dubbo.rpc.RpcException {
        if (arg0 == null) {
            throw new IllegalArgumentException("com.alibaba.dubbo.rpc.cluster.Directory argument == null");
        }
        if (arg0.getUrl() == null) {
            throw new IllegalArgumentException("com.alibaba.dubbo.rpc.cluster.Directory argument getUrl() == null");
        }
        com.alibaba.dubbo.common.URL url = arg0.getUrl();
        String extName = url.getParameter("cluster", "failover");
        if (extName == null) {
            throw new IllegalStateException("Fail to get extension(com.alibaba.dubbo.rpc.cluster.Cluster) name from url(" + url.toString() + ") use keys([cluster])");
        }
        com.alibaba.dubbo.rpc.cluster.Cluster extension = (com.alibaba.dubbo.rpc.cluster.Cluster)
                // 这里得到的是MockClusterWrapper->FailoverCluster;
                // join后得到的是MockClusterInvoker—>FailoverClusterInvoker invoker
                ExtensionLoader.getExtensionLoader(com.alibaba.dubbo.rpc.cluster.Cluster.class).getExtension(extName);
        return extension.join(arg0);
    }
}