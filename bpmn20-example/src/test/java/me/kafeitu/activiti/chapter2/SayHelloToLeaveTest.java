package me.kafeitu.activiti.chapter2;

import org.activiti.engine.*;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;



/**
 * Activiti实战
 * 2.4 Hello World 例子 （P8）
 * 
 * @author zhengdan3
 *
 */
public class SayHelloToLeaveTest {

    @Test
    public void testStartProcess() throws Exception {
    	
    	// 创建流程引擎，使用内存数据库（创建一个使用H2内存数据库的流程引擎实例）
        ProcessEngine processEngine = ProcessEngineConfiguration
                .createStandaloneInMemProcessEngineConfiguration()
                .buildProcessEngine();

        
        // 部署流程定义文件
        RepositoryService repositoryService = processEngine.getRepositoryService();
        String bpmnFileName = "me/kafeitu/activiti/helloworld/SayHelloToLeave.bpmn";

        // deploy() 将所有提供的资源部署到Activiti引擎
        repositoryService.createDeployment().addInputStream("SayHelloToLeave.bpmn",
                        this.getClass().getClassLoader().getResourceAsStream(bpmnFileName)).deploy();

        
        // 验证已部署流程定义 （验证部署的流程是否成功）
        ProcessDefinition processDefinition = repositoryService
                .createProcessDefinitionQuery().singleResult();
        assertEquals("SayHelloToLeave", processDefinition.getKey());

        // 启动流程并返回流程实例
        RuntimeService runtimeService = processEngine.getRuntimeService();

        // 构建流程中使用的变量
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("applyUser", "employee1"); // 申请人姓名
        variables.put("days", 3); // 请假天数

        // 启动流程，同时设置流程变量 (启动流程时会把这两个变量存入数据库中，以后可以通过接口读取到节点)
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                "SayHelloToLeave", variables);
        assertNotNull(processInstance);
        System.out.println("pid=" + processInstance.getId() + ", pdid=" + processInstance.getProcessDefinitionId());


        // TaskService 提供对{@link Task}和表单相关操作的访问
        TaskService taskService = processEngine.getTaskService();
        
        // 节点设置了此任务由角色为 deptLeader的人员处理，既有 deptLeader角色的人员都可以处理此任务
        // singleResult() 执行查询并返回结果实体，如果没有实体匹配查询条件则返回null
        Task taskOfDeptLeader = taskService.createTaskQuery()
                .taskCandidateGroup("deptLeader").singleResult();
        assertNotNull(taskOfDeptLeader);
        assertEquals("领导审批", taskOfDeptLeader.getName());

        
        // 调用claim方法“签收”，此任务归用户leaderUser所有
        // 在设计时，指定了由leaderUser处理（<userTask id="usertask1" name="领导审批" activiti:candidateGroups="deptLeader"></userTask>）
        taskService.claim(taskOfDeptLeader.getId(), "leaderUser");
        
        // 领导处理的结果
        variables = new HashMap<String, Object>();
        variables.put("approved", true);
        // 当任务成功执行时调用，所需的任务参数由终端用户给出
        taskService.complete(taskOfDeptLeader.getId(), variables);

        
        // 为了让读者更好理解执行过程，因为任务已经办理完成，再次查询组deptLeader的任务已经为空
        taskOfDeptLeader = taskService.createTaskQuery().taskCandidateGroup("deptLeader").singleResult();
        assertNull(taskOfDeptLeader);

        
        // 通过流程引擎对象获取历史记录查询接口
        HistoryService historyService = processEngine.getHistoryService();
        
        // 通过历史记录接口统计已经完成（finished）的流程实例数量，并验证预期结果
        long count = historyService.createHistoricProcessInstanceQuery().finished()
                .count();
        assertEquals(1, count);
    }
}


/*
 * Groovy 语言快速入门
 * https://www.jianshu.com/p/e8dec95c4326
 * 
 */









