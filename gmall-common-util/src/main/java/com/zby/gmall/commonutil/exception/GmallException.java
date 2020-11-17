package com.zby.gmall.commonutil.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GmallException extends RuntimeException {
    private Integer code;//状态吗
    private String message;//返回信息
}
