package com.tanle.t_shorten_url.cache;

public interface DistributeLockService {

    Boolean acquireLock(String key);

    void releaseLock(String key);
}
