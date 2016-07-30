/*
 * Copyright 1999-2015 dangdang.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package com.dangdang.ddframe.job.lite.internal.schedule;

import com.dangdang.ddframe.job.lite.api.config.LiteJobConfiguration;
import com.dangdang.ddframe.job.lite.api.listener.ElasticJobListener;
import com.dangdang.ddframe.job.lite.internal.config.ConfigurationService;
import com.dangdang.ddframe.job.lite.internal.election.LeaderElectionService;
import com.dangdang.ddframe.job.lite.internal.execution.ExecutionService;
import com.dangdang.ddframe.job.lite.internal.listener.ListenerManager;
import com.dangdang.ddframe.job.lite.internal.monitor.MonitorService;
import com.dangdang.ddframe.job.lite.internal.server.ServerService;
import com.dangdang.ddframe.job.lite.internal.sharding.ShardingService;
import com.dangdang.ddframe.reg.base.CoordinatorRegistryCenter;

import java.util.List;

/**
 * 为调度器提供内部服务的门面类.
 * 
 * @author zhangliang
 */
public class SchedulerFacade {
    
    private final ConfigurationService configService;
    
    private final LeaderElectionService leaderElectionService;
    
    private final ServerService serverService;
    
    private final ShardingService shardingService;
    
    private final ExecutionService executionService;
    
    private final MonitorService monitorService;
    
    private final ListenerManager listenerManager;
    
    public SchedulerFacade(final CoordinatorRegistryCenter regCenter, final LiteJobConfiguration liteJobConfig, final List<ElasticJobListener> elasticJobListeners) {
        configService = new ConfigurationService(regCenter, liteJobConfig);
        leaderElectionService = new LeaderElectionService(regCenter, liteJobConfig);
        serverService = new ServerService(regCenter, liteJobConfig);
        shardingService = new ShardingService(regCenter, liteJobConfig);
        executionService = new ExecutionService(regCenter, liteJobConfig);
        monitorService = new MonitorService(regCenter, liteJobConfig);
        listenerManager = new ListenerManager(regCenter, liteJobConfig, elasticJobListeners);
    }
    
    /**
     * 每次作业启动前清理上次运行状态.
     */
    public void clearPreviousServerStatus() {
        serverService.clearPreviousServerStatus();
    }
    
    /**
     * 注册Elastic-Job启动信息.
     */
    public void registerStartUpInfo() {
        listenerManager.startAllListeners();
        leaderElectionService.leaderForceElection();
        configService.persist();
        serverService.persistServerOnline();
        serverService.clearJobPausedStatus();
        shardingService.setReshardingFlag();
        monitorService.listen();
    }
    
    /**
     * 释放作业占用的资源.
     */
    public void releaseJobResource() {
        monitorService.close();
        serverService.removeServerStatus();
    }
    
    /**
     * 读取作业配置.
     *
     * @return 作业配置
     */
    public LiteJobConfiguration loadJobConfiguration() {
        return configService.load(false);
    }
    
    /**
     * 获取作业触发监听器.
     * 
     * @return 作业触发监听器
     */
    public JobTriggerListener newJobTriggerListener() {
        return new JobTriggerListener(executionService, shardingService);
    }
}