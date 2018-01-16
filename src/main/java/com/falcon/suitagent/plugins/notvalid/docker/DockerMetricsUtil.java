/*
 * www.yiji.com Inc.
 * Copyright (c) 2016 All Rights Reserved
 */
package com.falcon.suitagent.plugins.notvalid.docker;
/*
 * 修订记录:
 * guqiu@yiji.com 2016-08-10 11:33 创建
 */

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.falcon.suitagent.util.CommandUtilForUnix;
import com.falcon.suitagent.util.HttpUtil;

import java.io.IOException;

/**
 * @author guqiu@yiji.com
 */
class DockerMetricsUtil {

    private String urlPrefix;

    /**
     * 创建实例
     * @param cAdvisorIp
     * @param cAdvisorPort
     */
    DockerMetricsUtil(String cAdvisorIp, int cAdvisorPort) {
        this.urlPrefix = "http://" + cAdvisorIp + ":" + cAdvisorPort + "/";
    }

    /**
     * 获取所有的容器
     * @return
     * @throws IOException
     */
    JSONObject getContainersJSON() throws IOException {
        String url = urlPrefix + "api/v1.2/docker/";
        return JSON.parseObject(HttpUtil.get(url,30000,30000).getResult());
    }

    /**
     * 获取机器信息
     * @return
     * @throws IOException
     */
    JSONObject getMachineInfo() throws IOException {
        String url = urlPrefix + "api/v2.0/machine";
        return JSON.parseObject(HttpUtil.get(url,30000,30000).getResult());
    }

    /**
     * 在指定容器中执行指定命令
     * @param cmd
     * 执行的命令
     * @param containerIdOrName
     * 目标容器id或name
     * @return
     * @throws IOException
     */
    synchronized DockerExecResult exec(String cmd,String containerIdOrName) throws IOException {
        DockerExecResult execResult = new DockerExecResult();

        String dockerExecCmd = String.format("docker exec %s %s",containerIdOrName,cmd);
        CommandUtilForUnix.ExecuteResult executeResult = CommandUtilForUnix.execWithReadTimeLimit(dockerExecCmd,false,7);
        if(!executeResult.isSuccess){
            execResult.setSuccess(false);
        }else{
            String msg = executeResult.msg;
            if(msg.startsWith("rpc error")){
                execResult.setSuccess(false);
                execResult.setResult(msg);
            }else{
                execResult.setSuccess(true);
                execResult.setResult(msg);
            }
        }

        return execResult;
    }

}
