package vn.shop.auth.service;

import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import vn.shop.common.ApiException;

@Component
public class AttemptLimiter {
    private record Window(long start,int count) {}
    private final ConcurrentHashMap<String,Window> windows=new ConcurrentHashMap<>();
    public void check(String key) {
        long now=System.currentTimeMillis();
        if(windows.size()>10000) windows.entrySet().removeIf(e -> now-e.getValue().start()>60000);
        if(windows.size()>20000 && !windows.containsKey(key)) throw new ApiException(429,"Quá nhiều yêu cầu. Thử lại sau một phút");
        var w=windows.compute(key,(k,old) -> old==null || now-old.start()>60000 ? new Window(now,1) : new Window(old.start(),old.count()+1));
        if(w.count()>15) throw new ApiException(429,"Quá nhiều yêu cầu. Thử lại sau một phút");
    }
}
