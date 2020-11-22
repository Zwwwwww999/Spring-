package com.alone.springhandle.config;
import com.alone.springhandle.annotation.AloneAutowired;
import com.alone.springhandle.annotation.AloneController;
import com.alone.springhandle.annotation.AloneRequestMapping;
import com.alone.springhandle.annotation.AloneService;
import org.springframework.stereotype.Service;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

/**
 * @ClassName AloneDispatcherServlet
 * @Author zzzzwwwwwwwwwwwwww
 * @Date 2020/11/22 14:33
 * @Description AloneDispatcherServlet
 * @Version 1.0
 */
public class AloneDispatcherServlet extends HttpServlet {
    // 保存配置文件
    private Properties contextConfig = new Properties();
    // 保存类名
    private List<String> classNames=new ArrayList<>();

    // 伪IOC容器
    private Map<String,Object> ioc=new HashMap<>();

    //保存url与method的对应关系
    private Map<String,Method> handlerMap = new HashMap<> ();
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doGet(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doDispath(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("500 Exection,Detail"+Arrays.toString(e.getStackTrace()));
        }
        // 6. 调用运行阶段
        this.doPost(req, resp);
    }

    private void doDispath(HttpServletRequest req, HttpServletResponse resp)throws Exception {
        // 绝对路径
        String url =req.getRequestURI();

        // 处理成相对路径
        String contextPath = req.getContextPath();

        url=url.replace(contextPath,"").replaceAll("/+","/");

        if (!this.handlerMap.containsKey(url)){
                resp.getWriter().write("404 NOT FOUND");
                return;
        }

        Method method = this.handlerMap.get(url);

        // 投机取巧的方式，通过反射拿到method所在的class的名称，再调用转换首字母的方法
        String beanName = toLowerFirstCase(method.getDeclaringClass().getSimpleName());

        // 暂时写死
        Map<String,String[]> params =req.getParameterMap();
        method.invoke(ioc.get(beanName),new Object[]{req,resp,params.get("name")[0]});
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
       //1、加载配置文件
        doLoadConfig(config.getInitParameter("contextConfigLocation"));

        //2. 扫描相关的类
        doScanner(contextConfig.getProperty("scanPackage"));
        //3、初始化扫描到的类，将他们放入到IOC容器中
        doInstance();

        //4、完成依赖注入
        doAutowired();

        //5. 初始化HandlerMapping
        initHandlerMapping();

        System.out.println("Alone Spring framework is init");
    }

    // 初始化url和Methodd的一对一的关系
    private void initHandlerMapping() {
        if (ioc.isEmpty()){return;}
        ioc.entrySet().forEach(entry -> {
            Class<?> clazz = entry.getValue().getClass();

            if (!clazz.isAnnotationPresent(AloneController.class)){return;}

            // 用于保存类上的url
            String baseUrl="";
            if (clazz.isAnnotationPresent(AloneRequestMapping.class)){
                AloneRequestMapping aloneRequestMapping = clazz.getAnnotation(AloneRequestMapping.class);
                baseUrl =aloneRequestMapping.name();
            }

            // 默认获取所有的public方法
            for (Method method:clazz.getMethods()
                 ) {
                    if(!method.isAnnotationPresent(AloneRequestMapping.class)){continue;}

                AloneRequestMapping aloneRequestMapping = method.getAnnotation(AloneRequestMapping.class);
                    String url=baseUrl+aloneRequestMapping.name().replaceAll("/+","/");
                handlerMap.put(url,method);
                System.out.println("Mapped"+url+","+method);

            }
        });
    }

    private void doAutowired() {

        if (ioc.isEmpty()){
            return;
        }

        ioc.entrySet().forEach(entry -> {

            // 所有的特定的字段，包括private/protected/default
            Field[] fields = entry.getKey().getClass().getDeclaredFields();
            for (Field field : fields) {
                if (!field.isAnnotationPresent(AloneAutowired.class)){
                    continue;
                }
                AloneAutowired autowired = field.getAnnotation(AloneAutowired.class);
                String beanName = autowired.value().trim();
                // 如果未自定义beanName，则根据类型注入
                if ("".equals(beanName)){
                    beanName=field.getType().getName();
                }

                 field.setAccessible(true);
                try {
                    // 利用反射完成动态字段装配
                    field.set(entry.getKey(), ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

            }

        });
    }
    // 扫描出相关的类
    private void doScanner(String scanPackage) {

        // scanPackage = com.alone.demo
         URL url=this.getClass().getClassLoader().getResource("/"+scanPackage.replaceAll("\\.","/"));
        File classPath=new File(url.getFile());

        for (File file : classPath.listFiles()) {

            if (file.isDirectory()){
                doScanner(scanPackage+"."+file.getName());
            }else {
                if (!file.getName().endsWith(".class")){
                    continue;
                }
                String className =(scanPackage+"."+file.getName().replace(".class",""));
                classNames.add(className);
            }
        }
    }

    private void doInstance() {

         // 初始化，为DI做准备
        if (classNames.isEmpty()){return;}

        for (String className : classNames){
            try {
                Class<?> clazz =Class.forName(className);
                // 被注解标记的类才进行初始化
                // @Compent 等注解类似
                if (clazz.isAnnotationPresent(AloneController.class)){
                    Object instance =clazz.newInstance();
                    // spring默认为首字母小写
                        String beanName=toLowerFirstCase(clazz.getSimpleName());
                    ioc.put(clazz.getSimpleName(), instance);
                }else if (
                clazz.isAnnotationPresent(AloneService.class)){

                    // 1.自定义的beanname
                    AloneService service = clazz.getAnnotation(AloneService.class);
                    String  beanName=service.value();


                    // 2. 默认类名首字母小写
                    if ("".equals(beanName.trim())){
                        beanName = toLowerFirstCase(clazz.getSimpleName());

                    }

                    Object instance = clazz.newInstance();
                    ioc.put(beanName,instance);
                    // 3. 根据类型注入
                    for (Class<?> i: clazz.getInterfaces()){
                        if (ioc.containsKey(i.getName())){
                            throw new Exception("beanName is exits");
                        }
                                ioc.put(i.getName(),instance);
                    }

                }else {
                    continue;
                }


            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String toLowerFirstCase(String simpleName) {
            char [] chars=simpleName.toCharArray();
            // 之所以加32，是因为大小写字母的ASCII码相差32，而且大写字母的ASCII码要小于小写
            chars[0]+=32;
            return String.valueOf(chars);
    }

    // 加载配置文件
    private void doLoadConfig(String contextConfigLocation) {

        // 从类路径下找到Spring主配置文件所在的路径 并且将其读取出来放在Properties对象中
        InputStream fis=null;
         fis=this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);

        try {
            contextConfig.load(fis);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
