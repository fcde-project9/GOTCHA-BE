package com.gotcha.domain.admin.exception;

import com.gotcha._global.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@Slf4j
@Order(1)
@ControllerAdvice(basePackages = "com.gotcha.domain.admin")
public class AdminExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ModelAndView handleBusinessException(BusinessException e) {
        log.warn("Admin BusinessException: {}", e.getMessage());
        ModelAndView mav = new ModelAndView("admin/error");
        mav.setStatus(HttpStatus.BAD_REQUEST);
        mav.addObject("errorMessage", e.getMessage());
        mav.addObject("errorCode", e.getErrorCode().getCode());
        return mav;
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleException(Exception e) {
        log.error("Admin unhandled exception: ", e);
        ModelAndView mav = new ModelAndView("admin/error");
        mav.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        mav.addObject("errorMessage", "서버 오류가 발생했습니다.");
        return mav;
    }
}
