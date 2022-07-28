/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.identity.rest.api.user.approval.v1.impl;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.wso2.carbon.identity.api.user.approval.common.ApprovalConstant;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.rest.api.user.approval.v1.MeApiService;
import org.wso2.carbon.identity.rest.api.user.approval.v1.core.UserApprovalService;
import org.wso2.carbon.identity.rest.api.user.approval.v1.dto.TaskSummaryDTO;
import org.wso2.carbon.identity.workflow.engine.SimpleWorkflowEngineApprovalService;
import org.wso2.carbon.identity.workflow.engine.dto.StateDTO;
import org.wso2.carbon.identity.workflow.engine.dto.TaskDataDTO;
import org.wso2.carbon.identity.workflow.engine.internal.dao.WorkflowEventRequestDAO;
import org.wso2.carbon.identity.workflow.engine.internal.dao.impl.WorkflowEventRequestDAOImpl;
import org.wso2.carbon.identity.workflow.mgt.bean.Workflow;
import org.wso2.carbon.identity.workflow.mgt.dao.WorkflowDAO;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.ws.rs.core.Response;

/**
 * API service implementation of a logged in user's approval operations
 */
public class MeApiServiceImpl extends MeApiService {

    private SimpleWorkflowEngineApprovalService simpleWorkflowEngineApprovalService;
    private UserApprovalService userApprovalService;
    private static boolean enableApprovalsFromSimpleWorkflowEngine = Boolean.parseBoolean(IdentityUtil.getProperty(ApprovalConstant.SIMPLE_WORKFLOW_ENGINE_APPROVALS));
    private static boolean enableApprovalsFromBPEL = Boolean.parseBoolean(IdentityUtil.getProperty(ApprovalConstant.BPEL_ENGINE_APPROVALS));

    public MeApiServiceImpl() {

    }

    @Autowired
    public MeApiServiceImpl(SimpleWorkflowEngineApprovalService simpleWorkflowEngineApprovalService, UserApprovalService userApprovalService) {

        super();
        this.simpleWorkflowEngineApprovalService = simpleWorkflowEngineApprovalService;
        this.userApprovalService = userApprovalService;
    }

    @Override
    public Response getApprovalTaskInfo(String taskId) {

        WorkflowEventRequestDAO workflowEventRequestDAO = new WorkflowEventRequestDAOImpl();
        String taskDataDTO = workflowEventRequestDAO.getTask(taskId);
        if (taskDataDTO != null) {
            return Response.ok().entity(simpleWorkflowEngineApprovalService.getTaskData(taskId)).build();
        }
        return Response.ok().entity(userApprovalService.getTaskData(taskId)).build();
    }

    @Override
    public Response listApprovalTasksForLoggedInUser(Integer limit, Integer offset, List<String> status) {

        if (enableApprovalsFromSimpleWorkflowEngine && !enableApprovalsFromBPEL) {
            return Response.ok().entity(simpleWorkflowEngineApprovalService.listTasks(limit, offset, status)).build();
        } else if (enableApprovalsFromBPEL && !enableApprovalsFromSimpleWorkflowEngine) {
            return Response.ok().entity(userApprovalService.listTasks(limit, offset, status)).build();
        }
        List<TaskSummaryDTO> BPELApprovalList = userApprovalService.listTasks(limit, offset, status);
        List<org.wso2.carbon.identity.workflow.engine.dto.TaskSummaryDTO> simpleWorkflowEngineApprovalList =
                simpleWorkflowEngineApprovalService.listTasks(limit, offset, status);
        List<Object> allPendingList = Stream.concat(BPELApprovalList.stream(), simpleWorkflowEngineApprovalList.
                stream()).collect(Collectors.toList());
        return Response.ok().entity(allPendingList).build();
    }

    @Override
    public Response updateStateOfTask(String taskId, StateDTO nextState) {

        WorkflowEventRequestDAO workflowEventRequestDAO = new WorkflowEventRequestDAOImpl();
        String taskDataDTO = workflowEventRequestDAO.getTask(taskId);
        if (taskDataDTO != null) {
            simpleWorkflowEngineApprovalService.updateStatus(taskId, nextState);
            return Response.ok().build();
        }
        new UserApprovalService().updateStatus(taskId, nextState);
        return Response.ok().build();
    }
}
