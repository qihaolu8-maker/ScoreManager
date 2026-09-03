// 实体类：日志数据模型
public class LogEntry {
    public String time;
    public String className;
    public String id;
    public String name;
    public String change;
    public String remark;

    public LogEntry(String time, String className, String id, String name, String change, String remark) {
        this.time = time;
        this.className = className;
        this.id = id;
        this.name = name;
        this.change = change;
        this.remark = remark;
    }
}