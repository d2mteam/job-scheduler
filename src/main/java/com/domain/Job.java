package com.domain;

import com.infra.InfraJobContext;

public interface Job {
    void prepare(JobContext context, InfraJobContext infraContext) throws Exception;

    void execute(JobContext context, InfraJobContext infraContext) throws Exception;

    void rollback(JobContext context, InfraJobContext infraContext, Exception cause);
}
