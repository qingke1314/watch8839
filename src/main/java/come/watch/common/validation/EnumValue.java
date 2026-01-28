package come.watch.common.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 枚举值校验注解（Bean Validation / JSR-380）。
 *
 * 用途：校验某个字段的值是否“属于指定枚举”的合法取值集合。
 *
 * 典型用法：
 *
 * <pre>{@code
 * public class Query {
 *   // 例如：前端传 1/2/3，这里用 Metric.getCode() 来判断是否合法
 *   @EnumValue(enumClass = Metric.class, method = "getCode", message = "metric非法")
 *   private Integer metric;
 * }
 * }</pre>
 *
 * 实现原理：
 * - 校验时由 {@link EnumValueValidator} 遍历 enumClass 的所有常量
 * - 对每个常量，反射调用 {@link #method()} 指定的无参方法（如 getCode/getValue/name）取出“枚举值”
 * - 如果任意一个枚举值与字段值 {@code Objects.equals} 相等，则校验通过
 *
 * 空值策略：
 * - {@code null} 视为通过（必填请配合 {@code @NotNull/@NotBlank}）
 */
@Documented
// target限定使用位置，FIELD：字段上（dto等），PARAMETER：方法参数上
@Target({ElementType.FIELD, ElementType.PARAMETER})
// retention指定注解在什么阶段还保留着 ，RUNTIME：运行时保留，jvm通过反射去拿
@Retention(RetentionPolicy.RUNTIME)
//- 作用：告诉 Bean Validation：这是一个“约束注解”，并且由哪个校验器来实现校验逻辑。
//  validatedBy = EnumValueValidator.class 意味着：                                                                                                                            ▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀
//  当字段上标了 @EnumValue(...)                                                                                                                                                      ctrl+t variants  tab agents  ctrl+p commands
//  校验框架会调用 EnumValueValidator.initialize() 读取注解参数
//  再调用 EnumValueValidator.isValid() 来判断字段值是否通过校验
@Constraint(validatedBy = EnumValueValidator.class)
public @interface EnumValue {

    /**
     * 指定用于校验的枚举类型。
     *
     * 例如：{@code Metric.class}。
     */
    Class<? extends Enum<?>> enumClass();


    /**
     * 指定枚举上用于“取值”的方法名（必须是 public 且无参）。
     *
     * 常见取值：
     * - {@code "name"}：使用枚举常量名（如 "LCP"）
     * - {@code "getCode"}：使用业务 code（如 1/2/3）
     *
     * 该方法的返回类型需要能与被注解字段类型进行 equals 比较。
     */
    String method() default "name";


    /**
     * 校验失败时的错误提示。
     */
    String message() default "value非法";


    /**
     * Bean Validation 标准参数：groups。
     */
    Class<?>[] groups() default {};


    /**
     * Bean Validation 标准参数：payload。
     */
    Class<? extends Payload>[] payload() default {};
}
