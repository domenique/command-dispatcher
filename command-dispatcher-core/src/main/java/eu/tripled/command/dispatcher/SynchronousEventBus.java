package eu.tripled.command.dispatcher;

import com.google.common.base.Preconditions;
import eu.tripled.command.*;
import eu.tripled.command.callback.CommandValidationException;
import eu.tripled.command.callback.ExceptionThrowingEventCallback;
import eu.tripled.command.interceptor.SimpleInterceptorChain;
import org.reflections.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Synchronous implementation of the CommandDispatcher.
 */
public class SynchronousEventBus implements Publisher, Subscriber {

  private static final Logger LOGGER = LoggerFactory.getLogger(SynchronousEventBus.class);

  private ConcurrentMap<Class<?>, EventHandlerInvoker> eventHandlers;
  private List<EventBusInterceptor> interceptors = new ArrayList<>();

  public SynchronousEventBus() {
    this.eventHandlers = new ConcurrentHashMap<>();
    this.interceptors = new ArrayList<>();
  }

  public SynchronousEventBus(List<EventBusInterceptor> interceptors) {
    this.eventHandlers = new ConcurrentHashMap<>();
    this.interceptors = interceptors;
  }

  @Override
  public void subscribe(Object eventHandler) {
    Set<Method> methods = ReflectionUtils.getAllMethods(eventHandler.getClass(),
        ReflectionUtils.withAnnotation(Handles.class));

    for (Method method : methods) {
      Handles annotation = method.getAnnotation(Handles.class);
      subscribeInternal(eventHandler, annotation.value(), method);
    }
  }

  @Override
  public <ReturnType> void publish(Object message, EventCallback<ReturnType> callback) {
    Preconditions.checkArgument(message != null, "The message cannot be null.");
    Preconditions.checkArgument(callback != null, "The callback cannot be null.");
    LOGGER.debug("Received a message to publish: {}", message.getClass().getSimpleName());

    dispatchInternal(new Command<>(message), callback);

    LOGGER.debug("Finished executing commandMessage {}", message.getClass().getSimpleName());
  }

  @Override
  public void publish(Object message) {
    publish(message, new ExceptionThrowingEventCallback<>());
  }

  protected void subscribeInternal(Object eventHandler, Class<?> command, Method method) {
    if (LOGGER.isWarnEnabled() && eventHandlers.containsKey(command)) {
      LOGGER.warn("Subscription for {} already exists. Silently overwriting previous subscription.", command.getSimpleName());
    }
    eventHandlers.put(command, new EventHandlerInvoker(eventHandler, method));
  }

  protected <ReturnType> void dispatchInternal(Command command, EventCallback<ReturnType> callback) {
    EventHandlerInvoker invoker = findEventHandler(command);
    InterceptorChain<ReturnType> chain = createChain(command, invoker);

    try {
      ReturnType response = chain.proceed();
      callback.onSuccess(response);
    } catch (CommandValidationException validationEx) {
      callback.onValidationFailure(command);
    } catch (Throwable exception) {
      callback.onFailure(exception);
    }
  }

  private EventHandlerInvoker findEventHandler(Command command) {
    EventHandlerInvoker invoker = eventHandlers.get(command.getBody().getClass());
    if (invoker == null) {
      throw new EventHandlerNotFoundException(String.format("Could not find a command handler for %s", command.getBody().getClass().getSimpleName()));
    }
    return invoker;
  }


  protected <ReturnType> SimpleInterceptorChain<ReturnType> createChain(Command command, EventHandlerInvoker invoker) {
    return new SimpleInterceptorChain<>(command, invoker, interceptors);
  }
}
