package come.watch.common.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * {@link EnumValue} 的具体校验器。
 *
 * 校验逻辑：
 * - 允许 null（与 @NotNull/@NotBlank 组合使用）
 * - 从注解中读取 enumClass 与 method
 * - 反射获取 enum 上的无参方法（method 指定）
 * - 遍历枚举常量，对每个常量执行 method()，拿到“枚举值”并与字段值比较
 *
 * 注意：
 * - 如果 method 不存在、或反射调用异常，当前实现会直接返回 false
 *   （意味着：配置错误时会导致该字段校验不通过，便于尽早暴露问题）
 */
public class EnumValueValidator implements ConstraintValidator<EnumValue, Object> {

    private Class<? extends Enum<?>> enumClass;
    private String method;

    /**
     * 初始化阶段：从注解上读取配置。
     */
    @Override
    public void initialize(EnumValue annotation) {
        this.enumClass = annotation.enumClass();
        this.method = annotation.method();
    }

    /**
     * 运行时校验。
     *
     * @param value   待校验的字段值
     * @param context 校验上下文（此处未自定义 message/template）
     * @return true=通过；false=不通过
     */
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        // 约束注解通常不处理“必填”，null 交给 @NotNull/@NotBlank
        if (value == null) {
            return true;
        }

        Method enumMethod;
        try {
            // 获取枚举上指定的无参 public 方法，例如 getCode()/getValue()/name()
            enumMethod = enumClass.getMethod(method);
        } catch (NoSuchMethodException e) {
            // method 配置错误：枚举上不存在该方法
            return false;
        }

        // 遍历枚举所有常量：只要任意一个常量的“枚举值”与字段值相等即通过
        for (Enum<?> enumConstant : enumClass.getEnumConstants()) {
            Object enumValue;
            try {
                // 反射调用无参方法
                enumValue = enumMethod.invoke(enumConstant);
            } catch (Exception e) {
                // 反射调用失败：一般为权限/方法内部异常等
                return false;
            }
            if (Objects.equals(enumValue, value)) {
                return true;
            }
        }
        return false;
    }
}
