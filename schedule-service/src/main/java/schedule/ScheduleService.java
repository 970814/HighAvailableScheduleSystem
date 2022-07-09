package schedule;

import bean.ScheduleTask;
import bean.SubTask;
import db.TaskDbUtil;
import util.Utils;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;

// 调度服务
public class ScheduleService {
//    最大同时执行的定时任务数量
    int corePoolSize = 10;
    final ScheduledExecutorService scheduledExecutorService;
    Map<String, ScheduleTask> taskMap;// <taskId，ScheduleTask>
    Map<String, ScheduledFuture<?>> scheduledFutureMap; // 用于关闭任务
    public ScheduleService() {
//        一个是轮询数据库的线程
        scheduledExecutorService = Executors.newScheduledThreadPool(corePoolSize + 1);
        taskMap = new HashMap<>();
        scheduledFutureMap = new HashMap<>();
//        启动监听数据库线程
        listenTaskStatus();
    }

    private void listenTaskStatus() {
        //  每隔10s更新一次定时任务状态, 实现任务的启动、关闭、状态更新
        scheduledExecutorService
                .scheduleAtFixedRate(this::loadTaskStatusFromDB,
                        1, 10, TimeUnit.SECONDS);

    }


//    从数据库中载入任务状态
    private void loadTaskStatusFromDB() {
        try {
            List<ScheduleTask> scheduleTasks = TaskDbUtil.selectEnabledScheduleTask();
            Map<String, ScheduleTask> newTaskMap = new HashMap<>();
            for (ScheduleTask scheduleTask : scheduleTasks)
                newTaskMap.put(scheduleTask.getTaskId(), scheduleTask);

            update(taskMap, newTaskMap);//将数据库中的任务状态更新到内存

        } catch (SQLException | IOException e) {//暂不考虑异常情况
            throw new RuntimeException(e);
        }
    }

    //将数据库中的任务状态更新到内存
    private void update(Map<String, ScheduleTask> oldTaskMap, Map<String, ScheduleTask> newTaskMap) {
        Set<String> oldTaskIds = oldTaskMap.keySet();
        Set<String> newTaskIds = newTaskMap.keySet();

//        内存任务集合 减去 数据库任务集合 ---> 得到关闭的任务集合
        Set<String> disabledTaskIds = Utils.difference(oldTaskIds, newTaskIds); //被关闭的任务
        if (disabledTaskIds.size() > 0) { // 关闭任务
            long start = System.currentTimeMillis();
            System.out.println("需要关闭的任务有: " + disabledTaskIds);
            for (String disabledTaskId : disabledTaskIds) {
                scheduledFutureMap.remove(disabledTaskId).cancel(false);//会等到执行完成再从线程池中删除&关闭任务
                taskMap.remove(disabledTaskId);
            }
            System.out.println("任务已成功关闭, 耗时：" + (System.currentTimeMillis() - start) + "ms");
        }
//        数据库任务集合 减去 内存任务集合 ---> 得到启用的任务集合
        Set<String> enabledTaskIds = Utils.difference(newTaskIds, oldTaskIds);//被启用的任务
        if (enabledTaskIds.size() > 0) { // 启用任务
            for (String enabledTaskId : enabledTaskIds) {
                ScheduleTask scheduleTask = newTaskMap.get(enabledTaskId);
                startScheduleTask(scheduleTask);
            }
        }

        //交集部分，得到任务运行状态的变化   运行 -> 结束/失败
        //将内存中的任务运行状态和数据库中状态进行对比，以驱动DAG流程的执行
        Set<String> intersectionTaskIds = Utils.intersection(oldTaskIds, newTaskIds);
        for (String taskId : intersectionTaskIds) {//对每一个定时任务进行一个驱动
            ScheduleTask oldTask = oldTaskMap.get(taskId);
            ScheduleTask newTask = newTaskMap.get(taskId);
            taskStateTransition(oldTask, newTask);//任务状态的一个转换
        }
    }

    private void taskStateTransition(ScheduleTask oldTask, ScheduleTask newTask) {
        for (SubTask subTask : newTask.getSubTasks()) {

        }
    }

    //    启动定时任务
    private void startScheduleTask(ScheduleTask scheduleTask) {

        //将任务放入线程池
        ScheduledFuture<?> scheduledFuture = scheduledExecutorService
                .scheduleAtFixedRate(scheduleTask::run, 1, scheduleTask.getPeriod(), TimeUnit.MILLISECONDS);
        scheduledFutureMap.put(scheduleTask.getTaskId(), scheduledFuture); //用于关闭任务
        taskMap.put(scheduleTask.getTaskId(), scheduleTask);
    }

    public static void main(String[] args) {
        new ScheduleService();
    }


}
