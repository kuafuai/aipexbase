package com.kuafuai.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("users")
public class Users {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String email;
    private String phone;
    private String password;
    private String nickName;
    private String avator;
}
