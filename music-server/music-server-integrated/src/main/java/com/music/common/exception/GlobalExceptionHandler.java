package com.music.common.exception;

import com.music.common.core.domain.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartException;

import java.io.IOException;
import java.net.SocketTimeoutException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.error("业务异常: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "参数验证失败";
        log.error("参数验证失败: {}", message);
        return Result.error(400, message);
    }

    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "参数绑定失败";
        log.error("参数绑定失败: {}", message);
        return Result.error(400, message);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("非法参数异常: {}", e.getMessage());
        return Result.error(400, e.getMessage());
    }

    @ExceptionHandler(MultipartException.class)
    public Result<Void> handleMultipartException(MultipartException e, HttpServletRequest request) {
        String uri = request != null ? request.getRequestURI() : "unknown";
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        log.warn("文件上传请求格式错误: uri={}, message={}, root={} : {}",
                uri,
                e.getMessage(),
                root.getClass().getName(),
                root.getMessage());
        return Result.error(400, "上传失败：请使用 multipart/form-data 格式并包含 file 字段");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public Result<Void> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e, HttpServletRequest request) {
        String uri = request != null ? request.getRequestURI() : "unknown";
        String contentType = String.valueOf(e.getContentType());
        log.warn("请求Content-Type不支持: uri={}, contentType={}, supported={}", uri, contentType, e.getSupportedMediaTypes());
        return Result.error(415, "请求格式不支持：上传接口请使用 multipart/form-data");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Object handleNoResourceFoundException(NoResourceFoundException e, HttpServletRequest request, HttpServletResponse response) {
        String uri = request != null ? request.getRequestURI() : "unknown";
        // 对静态资源请求返回标准404，避免被全局异常包装成JSON导致客户端误判为可播放内容
        if (uri.startsWith("/static/")) {
            log.warn("静态资源不存在: {}", uri);
            if (response != null && !response.isCommitted()) {
                response.setStatus(HttpStatus.NOT_FOUND.value());
            }
            return null;
        }
        log.warn("资源不存在: {}", uri);
        return Result.error(404, "资源不存在");
    }

    @ExceptionHandler(Exception.class)
    public Object handleException(Exception e, HttpServletRequest request, HttpServletResponse response) {
        String uri = request != null ? request.getRequestURI() : "unknown";
        if (shouldSkipBodyWrite(e, request, response)) {
            return null;
        }

        // 静态资源异常不要包装为JSON，避免播放器将错误体当作媒体流解析
        if (uri.startsWith("/static/")) {
            log.error("静态资源访问异常: uri={}", uri, e);
            if (response != null && !response.isCommitted()) {
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            }
            return null;
        }

        // 磁盘/网络IO异常（如CRC校验错误），降级为WARN避免刷屏，不影响业务
        if (isMediaStreamIOException(e)) {
            log.warn("媒体流IO异常（可能为磁盘读取错误或客户端断开）: uri={}, msg={}", uri, e.getMessage());
            return null;
        }

        log.error("系统异常: ", e);
        return Result.error("系统异常，请联系管理员");
    }

    private boolean shouldSkipBodyWrite(Exception e, HttpServletRequest request, HttpServletResponse response) {
        if (response != null && response.isCommitted()) {
            log.warn("响应已提交，跳过异常响应写回。uri={}, ex={}",
                    request != null ? request.getRequestURI() : "unknown",
                    e.getClass().getSimpleName());
            return true;
        }

        String contentType = response != null ? response.getContentType() : null;
        if (isAudioContentType(contentType) && (isNetworkWriteException(e) || e instanceof HttpMessageNotWritableException)) {
            log.warn("音频流响应异常，跳过Result写回。uri={}, contentType={}, ex={}",
                    request != null ? request.getRequestURI() : "unknown",
                    contentType,
                    e.getClass().getSimpleName());
            return true;
        }

        return false;
    }

    private boolean isAudioContentType(String contentType) {
        return contentType != null && contentType.toLowerCase().startsWith("audio/");
    }

    private boolean isNetworkWriteException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SocketTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    /**
     * 判断是否为媒体流相关的IO异常（磁盘CRC错误、客户端关闭连接等）
     * 这类异常属于环境/硬件问题，降级为WARN处理
     */
    private boolean isMediaStreamIOException(Exception e) {
        // 检查调用栈中是否包含媒体流相关方法
        boolean isStreamContext = false;
        for (StackTraceElement element : e.getStackTrace()) {
            String className = element.getClassName();
            if (className.contains("ResourceRegionHttpMessageConverter")
                    || className.contains("ResourceHttpRequestHandler")
                    || className.contains("StreamUtils")) {
                isStreamContext = true;
                break;
            }
        }
        if (!isStreamContext) return false;

        // 检查是否为IO异常（包括CRC错误、连接重置等）
        Throwable current = e;
        while (current != null) {
            if (current instanceof IOException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
