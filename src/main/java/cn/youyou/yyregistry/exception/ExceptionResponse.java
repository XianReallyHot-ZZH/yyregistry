package cn.youyou.yyregistry.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
@AllArgsConstructor
public class ExceptionResponse {

    private HttpStatus httpStatus;
    private String message;

}
