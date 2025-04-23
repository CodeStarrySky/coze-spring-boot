# coze-spring-boot
基于coze-api 与 SpringBoot 的集成，简化coze的开发。

## 快速开始
### 引入依赖
```xml
<dependencies>
    <dependency>
        <groupId>com.wuch</groupId>
        <artifactId>coze-spring-boot-starter</artifactId>
        <version>${project.parent.version}</version>
    </dependency>
</dependencies>
```
### 修改配置
```yaml
coze:
  token: * * * * *
  botId: * * * * *
  baseUrl: * * * * *
  readTimeout: 100000
  connectTimeout: 100000
```

### 注入 ChatClient
```java
@RequestMapping("/api/assistant")
@RestController
@RequiredArgsConstructor
public class AssistantController {

    private final ChatClient agent;
    
    @RequestMapping(path="/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(String chatId, String userMessage) {
        return agent.chat(chatId, userMessage);
    }

}
```
### 信息存储配置
> 默认相关信息存储在内存中，如需持久化，可配置相关依赖。
> 
自定义配置：
```java
@Configuration
public class CozeConfig {

    @Bean
    public DataMemory redisDataMemory(StringRedisTemplate stringRedisTemplate) {
        return new RedisDataMemory(stringRedisTemplate);
    }


}

@RequiredArgsConstructor
// 实现DataMemory，并实现其中的方法即可
public class RedisDataMemory implements DataMemory {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void add(String key, String value) {
        stringRedisTemplate.opsForValue().set(key, value);
    }

    @Override
    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    @Override
    public void clear(String key) {
        stringRedisTemplate.delete(key);
    }
}
```


### Coze 端插件使用
#### metaData
> 元数据，在请求的时候可以传入metaData,响应中会把这个信息带过来，适合不需要给大模型，但插件需要的情景。
```java
	@RequestMapping(path="/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<String> chat(String chatId, String userMessage) {
		Map<String, String> metaData = new HashMap<>();
		metaData.put("userName", "test");
		metaData.put("userId", "userId");
		return agent.chat(chatId, userMessage, metaData);
	}
```
#### 方式1：不需要metaData的情况
```java
	@Bean
	public Function<BookingDetailsRequest, BookingDetails> getBookingDetails() {
		return request -> {
			try {
				return flightBookingService.getBookingDetails(request.bookingNumber(), request.name());
			}
			catch (Exception e) {
				logger.warn("Booking details: {}", NestedExceptionUtils.getMostSpecificCause(e).getMessage());
				return new BookingDetails(request.bookingNumber(), request.name(), null, null, null, null, null);
			}
		};
	}
```
#### 方式2: 适合需要metaData的情况
```java
@Service("changeBooking")
public class ChangeBookingService extends BaseToolCall<BookingTools.ChangeBookingDatesRequest, String> {
    @Autowired
    private FlightBookingService flightBookingService;
    
    // 如果不需要metaData，也可以重写call(T t)
    @Override
    protected String call(BookingTools.ChangeBookingDatesRequest request, Map<String, String> metaData) {
        flightBookingService.changeBooking(request.bookingNumber(), request.name(), request.date(), request.from(),
                request.to());
        return "处理成功";
    }
}
```



## 示例
* [Flight Booking Example](./coze-spring-boot-examples/flight-booking-example)