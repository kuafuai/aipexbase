package com.kuafuai.common.delay_task.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kuafuai.pay.db.LocalDateTimeTypeHandler;
import lombok.Data;
import org.apache.commons.lang3.ObjectUtils;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * @TableName delayed_tasks
 */
@TableName(value = "delayed_tasks")
@Data
public class DelayedTasks implements Serializable {
    /**
     *
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     *
     */
    private String taskType;

    /**
     *
     */
    private String taskData;

    /**
     *
     */
    private String status;

    @TableField(typeHandler = LocalDateTimeTypeHandler.class)
    /**
     *
     */
    private LocalDateTime executeTime;
    @TableField(typeHandler = LocalDateTimeTypeHandler.class)
    /**
     *
     */
    private LocalDateTime createTime;
    @TableField(typeHandler = LocalDateTimeTypeHandler.class)
    /**
     *
     */
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;


    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("task_type", taskType);
        map.put("task_data", taskData);
        map.put("status", status);
        map.put("execute_time", executeTime);
        map.put("create_time", createTime);
        map.put("update_time", updateTime);
        return map;
    }

    public Map<String, Object> toMapNotNull() {
        Map<String, Object> map = new HashMap<>();
        addDateToMap(map, "id", id);
        addDateToMap(map, "task_type", taskType);
        addDateToMap(map, "task_data", taskData);
        addDateToMap(map, "status", status);
        addDateToMap(map, "execute_time", executeTime);
        addDateToMap(map, "create_time", createTime);
        addDateToMap(map, "update_time", updateTime);
        return map;
    }

    private void addDateToMap(Map<String, Object> map, String key, Object value) {
        if (ObjectUtils.isNotEmpty(value)) {
            map.put(key, value);
        }
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        DelayedTasks other = (DelayedTasks) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
                && (this.getTaskType() == null ? other.getTaskType() == null : this.getTaskType().equals(other.getTaskType()))
                && (this.getTaskData() == null ? other.getTaskData() == null : this.getTaskData().equals(other.getTaskData()))
                && (this.getStatus() == null ? other.getStatus() == null : this.getStatus().equals(other.getStatus()))
                && (this.getExecuteTime() == null ? other.getExecuteTime() == null : this.getExecuteTime().equals(other.getExecuteTime()))
                && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()))
                && (this.getUpdateTime() == null ? other.getUpdateTime() == null : this.getUpdateTime().equals(other.getUpdateTime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getTaskType() == null) ? 0 : getTaskType().hashCode());
        result = prime * result + ((getTaskData() == null) ? 0 : getTaskData().hashCode());
        result = prime * result + ((getStatus() == null) ? 0 : getStatus().hashCode());
        result = prime * result + ((getExecuteTime() == null) ? 0 : getExecuteTime().hashCode());
        result = prime * result + ((getCreateTime() == null) ? 0 : getCreateTime().hashCode());
        result = prime * result + ((getUpdateTime() == null) ? 0 : getUpdateTime().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", taskType=").append(taskType);
        sb.append(", taskData=").append(taskData);
        sb.append(", status=").append(status);
        sb.append(", executeTime=").append(executeTime);
        sb.append(", createTime=").append(createTime);
        sb.append(", updateTime=").append(updateTime);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}