# 高可用任务调度系统

**架构图v0.2**
<img width="1283" alt="ArchitectureDiagram" src="https://user-images.githubusercontent.com/19931702/180603146-dc0c6681-2bfc-41f4-b5a1-351502aa5d82.png">

#### ~ 2022-07-03

1. 完成了定时任务和子任务的类、数据表的定义
2. 上传任务到数据库的编码
3. 完成了定时任务的启动和停止
4. 完成了部分DAG流程的执行

#### ~ 2022-07-10


1. 完成了DAG任务流程的执行&测试通过
2. 支持实时查看任务历史执行记录


#### ~ 2022-07-17

1. 更新数据改成事务
2. 实现了一个简单版本的ui查看界面
3. 基于zookeeper实现leader选举算法
4. 实现调度服务的高可用

#### ~ 2022-07-17

1. 新增对调度节点监控的swing-ui展示
2. 实现了worker节点


