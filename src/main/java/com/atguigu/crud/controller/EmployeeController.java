package com.atguigu.crud.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.atguigu.crud.bean.Employee;
import com.atguigu.crud.bean.Msg;
import com.atguigu.crud.service.EmployeeService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
/**
 * 处理员工CRUD请求
 * @author kaka
 *
 */
@Controller
public class EmployeeController {

	@Autowired
	EmployeeService employeeService;
	
	/**
	 * 单个/批量二合一删除方法
	 * 批量删除：1-2-3
	 * 单个删除：1
	 * 
	 * @param id
	 * @return
	 */
	@RequestMapping(value="/emp/{ids}", method=RequestMethod.DELETE)
	@ResponseBody
	public Msg deleteEmp(@PathVariable("ids")String ids) {
		if (ids.contains("-")) {
			// 批量删除
			String[] str_ids = ids.split("-");
			// 组装id的集合
			List<Integer> del_ids = new ArrayList<>();
			for (String  id : str_ids) {
				del_ids.add(Integer.parseInt(id));
			}
			employeeService.deleteBatch(del_ids);
		} else {
			// 单个删除
			Integer id = Integer.parseInt(ids);
			employeeService.deleteEmp(id);
		}
		
		return Msg.success();
	}
	
	/**
	 * 如果直接发送ajax=put形式的请求
	 * 封装的数据Employee [empId=1006, empName=null, gender=null, email=null, dId=null]
	 * 
	 * 问题：
	 * 		请求体中有数据，但是Employee对象封装不上；
	 * update tbl_emp where emp_id = 1006
	 * 
	 * 原因：
	 * 		1、Tomcat：将请求体中的数据，封装成一个map；
	 * 		2、request.getParameter("empName")就会从这个map中取值；
	 * 		3、SpringMVC封装POJO对象的时候，会把POJO中每个属性的值，通过调用request.getParameter()方法获取到；
	 * 
	 * AJAX发送PUT请求引发的问题：
	 * 		PUT请求，请求体中的数据，通过request.getParameter()方法拿不到；
	 * 		因为Tomcat一看是PUT请求就不会封装请求体中的数据为map，只有POST形式的请求才封装为map；
	 * 
	 * Tomcat7.0:
	 * 		org.apache.catalina.connector.Request--parseParameters() (第3147行)
	 * 		if (!getConnector().isParseBodyMethod(getMethod()) {
	 * 			success = true;
	 * 			return;
	 * 		}
	 * 
	 * 解决方案：
	 * 我们要能支持直接发送PUT之类的请求还要封装请求体中的数据
	 * 配置上HttpPutFormContentFilter；它的作用：将请求体中的数据解析包装成一个map；
	 * request被重新包装，request.getParameter()方法被重写，会从自己封装的map中取数据
	 * 
	 * 员工更新方法
	 * @param employee
	 * @return
	 */
	@RequestMapping(value="/emp/{empId}", method=RequestMethod.PUT)
	@ResponseBody
	public Msg saveEmp(Employee employee) {
		System.out.println("将要更新的员工数据：" + employee);
		employeeService.updateEmp(employee);
		return Msg.success();
	}
	
	/**
	 * 根据id查询员工
	 * @param id
	 * @return
	 */
	@RequestMapping(value="/emp/{id}", method=RequestMethod.GET)
	@ResponseBody
	public Msg getEmp(@PathVariable("id")Integer id) {
		Employee employee = employeeService.getEmp(id);
		return Msg.success().add("emp", employee);
	}
	
	/**
	 * 检查用户名是否可用
	 * @param empName
	 * @return
	 */
	@RequestMapping("/checkuser")
	@ResponseBody
	public Msg checkUser(@RequestParam("empName")String empName) {
		// 先判断用户名是否是合法的表达式
		String regex = "(^[a-zA-Z0-9_-]{6,16}$)|(^[\\u2E80-\\u9FFF]{2,5})";
		if (!empName.matches(regex)) {
			return Msg.fail().add("va_msg", "用户名必须是6-16位的字母和数字的组合或者是2-5位的中文");
		}
		
		// 数据库用户名重复校验
		boolean b = employeeService.checkUser(empName);
		if (b) {
			return Msg.success();
		} else {
			return Msg.fail().add("va_msg", "用户名不可用");
		}
	}
	
	
	/**
	 * 员工保存
	 * 
	 * 支持JSR303校验
	 * 需要导入hibernate-validator
	 * 
	 * @return
	 */
	@RequestMapping(value="/emp", method=RequestMethod.POST)
	@ResponseBody
	public Msg saveEmp(@Valid Employee employee, BindingResult result) {
		if (result.hasErrors()) {
			// 校验失败，返回失败信息，在模态框中显示校验失败的错误信息
			Map<String, Object> map = new HashMap<>();
			List<FieldError> errors = result.getFieldErrors();
			for (FieldError fieldError : errors) {
				System.out.println("错误的字段名：" + fieldError.getField());
				System.out.println("错误信息：" + fieldError.getDefaultMessage());
				map.put(fieldError.getField(), fieldError.getDefaultMessage());
			}
			return Msg.fail().add("errorFields", map);
		} else {
			employeeService.saveEmp(employee);
			return Msg.success();
		}
	}
	
	/**
	 * 使用ajax查询员工数据（分页查询），返回json字符串
	 * @RequestParam("pn")Integer pn	获取pn的值，然后赋给其后面的参数
	 * @RequestParam(value = "pn", defaultValue = "1")	设置pn的默认值为1
	 * 需要导入jackson包（jackson-databind），使用@ResponseBody自动地把返回的对象转为json字符串
	 * @param pn
	 * @return
	 */
	@RequestMapping("/emps")
	@ResponseBody
	public Msg getEmpWithJson(@RequestParam(value = "pn", defaultValue = "1")Integer pn) {
		// 引入PageHelper分页
		// 在查询之前只需要调用下面代码，传入页码，以及每页的显示纪录数
		PageHelper.startPage(pn, 5);
		// startPage后面紧跟的这个查询就是一个分页查询
		List<Employee> emps = employeeService.getAll();
		// 使用PageInfo包装查询后的结果，只需要将PageInfo交给页面就行了
		// PageInfo封装了详细的分页信息，包括有我们查询出来的数据
		PageInfo page = new PageInfo(emps, 5);	// 传入连续显示的页数
		return Msg.success().add("pageInfo", page);
	}
	
	/**
	 * 查询员工数据（分页查询）
	 * @RequestParam("pn")Integer pn	获取pn的值，然后赋给其后面的参数
	 * @RequestParam(value = "pn", defaultValue = "1")	设置pn的默认值为1
	 * @return
	 */
	// @RequestMapping("/emps")
	public String getEmps(@RequestParam(value = "pn", defaultValue = "1")Integer pn, Model model) {
		// 这不是一个分页查询
//		List<Employee> emps = employeeService.getAll();
		
		// 引入PageHelper分页
		// 在查询之前只需要调用下面代码，传入页码，以及每页的显示纪录数
		PageHelper.startPage(pn, 5);
		// startPage后面紧跟的这个查询就是一个分页查询
		List<Employee> emps = employeeService.getAll();
		// 使用PageInfo包装查询后的结果，只需要将PageInfo交给页面就行了
		// PageInfo封装了详细的分页信息，包括有我们查询出来的数据
		PageInfo page = new PageInfo(emps, 5);	// 传入连续显示的页数
		// model会保存在请求域中，随请求域传回到页面
		model.addAttribute("pageInfo", page);
		
		return "list";
	}
	
	
	/**
	 * 此方法为EasyUI演示数据分页的方法
	 * 
	 * 使用ajax查询员工数据（分页查询），返回json字符串
	 * @RequestParam("pn")Integer pn	获取pn的值，然后赋给其后面的参数
	 * @RequestParam(value = "pn", defaultValue = "1")	设置pn的默认值为1
	 * 需要导入jackson包（jackson-databind），使用@ResponseBody自动地把返回的对象转为json字符串
	 * @param pn
	 * @return
	 */
	@RequestMapping("/empsWithEasyUI")
	@ResponseBody
	public Map<String, Object> getEmpWithJsonAndEasyUI(@RequestParam(value = "page", defaultValue = "1")Integer page, 
			@RequestParam(value = "rows", defaultValue = "10")Integer rows) {
		// 引入PageHelper分页
		// 在查询之前只需要调用下面代码，传入页码，以及每页的显示纪录数
		PageHelper.startPage(page, rows);
		// startPage后面紧跟的这个查询就是一个分页查询
		List<Employee> emps = employeeService.getAll();
		// 使用PageInfo包装查询后的结果，只需要将PageInfo交给页面就行了
		// PageInfo封装了详细的分页信息，包括有我们查询出来的数据
		PageInfo pageinfo = new PageInfo(emps, 5);	// 传入连续显示的页数
		/*return Msg.success().add("pageInfo", pageinfo);*/
		Map<String, Object> result = new HashMap<>();
		result.put("total", pageinfo.getTotal());
		result.put("rows", emps);
		
		return result;
	}
}
