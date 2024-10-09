package com.xcs.unilock;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author xcs
 */
@Data
@AllArgsConstructor
public class UniLockResponse<T> {

    private String lockName;
    private String lockValue;
    private T instance;
}
