/*
 * www.yiji.com Inc.
 * Copyright (c) 2016 All Rights Reserved
 */
package com.falcon.suitagent.plugins.notvalid.zk;

import com.falcon.suitagent.util.CommandUtilForUnix;
import com.falcon.suitagent.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/*
 * 修订记录:
 * guqiu@yiji.com 2016-06-22 17:48 创建
 */

/**
 * @author guqiu@yiji.com
 */
@Slf4j
public class ZKConfig {

    private static ConcurrentHashMap<String,Map<String,Object>> cache = new ConcurrentHashMap<>();

    /**
     * 根据进程id获取zookeeper的配置文件配置
     * @param pid
     * zookeeper服务的进程id
     * @return
     * @throws IOException
     */
    private static Map<String,Object> getConfig(int pid) throws IOException {
        String key = String.valueOf(pid).intern();

        //读取缓存
        Map<String,Object> result = cache.get(key);
        if(result != null){
            return result;
        }else{
            result = new HashMap<>();
        }

        String cmd = "ls -al /proc/" + pid + "/fd/" + " | grep zookeeper";
        CommandUtilForUnix.ExecuteResult executeResult = CommandUtilForUnix.execWithReadTimeLimit(cmd,false,7);

        if(executeResult.isSuccess){
            String path = "";
            String msg = executeResult.msg;
            String[] ss = msg.split("\\s");
            for (String s : ss) {
                if(s.contains("zookeeper") &&
                        s.contains("jar") &&
                        s.substring(s.lastIndexOf("zookeeper"),s.lastIndexOf("jar")+3).
                                matches("zookeeper-[\\w*.]*\\.jar")){
                    path = s;
                    break;
                }
            }
            if(!"".equals(path)){
                path = path.substring(0,path.lastIndexOf(File.separator));
                path += File.separator + "conf" + File.separator + "zoo.cfg";

                Properties pps = new Properties();
                try (FileInputStream in = new FileInputStream(path)){
                    pps.load(in);
                    Enumeration en = pps.propertyNames(); //得到配置文件的名字
                    while(en.hasMoreElements()) {
                        String strKey = (String) en.nextElement();
                        String strValue = pps.getProperty(strKey);
                        result.put(strKey,strValue);
                    }
                    cache.put(key,result);
                } catch (IOException e) {
                    log.error("zookeeper配置文件查找失败,请检查是否路径存在空格",e);
                }

            }
        }else{
            log.error("命令 {} 执行失败,错误信息:\r\n{}",cmd,executeResult.msg);
        }
        return result;
    }

    /**
     * 获取ZK的clientPort端口号
     * @param pid
     * es的进程id
     * @return
     * @throws IOException
     */
    public static String getClientPort(int pid) throws IOException {
        String port = String.valueOf(getConfig(pid).get("clientPort"));
        if(StringUtils.isEmpty(port)){
            //未配置,返回默认配置值
            return "2182";
        }
        return port;
    }

}
