package io.github.halcyonsong.common.exception;

import io.github.halcyonsong.common.enums.ResultCodeEnum;
import io.github.halcyonsong.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<?> handleValidationException(Exception exception) {
        BindingResult bindingResult = null;

        if (exception instanceof MethodArgumentNotValidException) {
            bindingResult = ((MethodArgumentNotValidException) exception).getBindingResult();
        } else if (exception instanceof BindException) {
            bindingResult = ((BindException) exception).getBindingResult();
        }

        if (bindingResult == null || !bindingResult.hasErrors()) {
            log.warn("参数校验异常，但未获取到字段错误信息");
            return Result.error(ResultCodeEnum.PARAM_ERROR.getCode(), ResultCodeEnum.PARAM_ERROR.getMessage());
        }

        List<FieldError> fieldErrors = bindingResult.getFieldErrors();
        Map<String, String> errorMap = new LinkedHashMap<>();
        for (FieldError fieldError : fieldErrors) {
            errorMap.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        log.warn("参数校验异常: {}", errorMap);
        return Result.error(
                ResultCodeEnum.PARAM_ERROR.getCode(),
                ResultCodeEnum.PARAM_ERROR.getMessage(),
                errorMap
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<?> handleHttpMessageNotReadableException(HttpMessageNotReadableException exception) {
        log.warn("请求体格式异常: {}", exception.getMessage());
        return Result.error(ResultCodeEnum.PARAM_ERROR.getCode(), "请求参数格式错误");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<?> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException exception) {
        log.warn("缺少必传参数: {}", exception.getMessage());
        return Result.error(ResultCodeEnum.PARAM_ERROR.getCode(), "缺少必传参数");
    }

    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException exception) {
        log.warn("业务异常: code={}, message={}", exception.getCode(), exception.getMessage());
        return Result.error(exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<?> handleIllegalArgumentException(IllegalArgumentException exception) {
        log.warn("非法参数异常: {}", exception.getMessage());
        return Result.error(ResultCodeEnum.PARAM_ERROR.getCode(), exception.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<?> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException exception) {
        log.warn("上传文件超限: {}", exception.getMessage());
        return Result.error(ResultCodeEnum.PARAM_ERROR.getCode(), "上传文件大小超过限制");
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception exception) {
        log.error("系统发生未捕获异常", exception);
        return Result.error(ResultCodeEnum.SYSTEM_ERROR);
    }
}