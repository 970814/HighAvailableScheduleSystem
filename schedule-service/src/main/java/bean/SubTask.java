package bean;

import lombok.Data;

// 子任务
@Data
public class SubTask {
    String subTaskId;    // 子任务id，通常是.job文件的名称
    int activationValue; //激活值
    int startThreshold; //启动阈值
    int status; // 运行状态     结束0 等待1 运行2
    String command;//子任务的命令
}
