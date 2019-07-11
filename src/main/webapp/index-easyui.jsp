<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>EasyUI分页</title>
<%
	pageContext.setAttribute("APP_PATH", request.getContextPath());
%>
	<!-- 引入EasyUI -->
	<!-- 引入css文件 -->
	<link rel="stylesheet" type="text/css" href="${APP_PATH }/static/jquery-easyui-1.5.5.4/themes/default/easyui.css">
	<link rel="stylesheet" type="text/css" href="${APP_PATH }/static/jquery-easyui-1.5.5.4/themes/icon.css">
	<!-- 引入js文件 -->
	<script type="text/javascript" src="${APP_PATH }/static/jquery-easyui-1.5.5.4/jquery.min.js"></script>
	<script type="text/javascript" src="${APP_PATH }/static/jquery-easyui-1.5.5.4/jquery.easyui.min.js"></script>
</head>
<body>
	<table id="tt" class="easyui-datagrid" style="width:600px"
		url="${APP_PATH }/empsWithEasyUI"
		title="Load Data" iconCls="icon-save"
		rownumbers="true" pagination="true">
		<thead>
			<tr>
				<th field="empId" >empId</th>
				<th field="empName" >empName</th>
				<th field="gender" >gender</th>
				<th field="email" >email</th>
				<th field="deptName" >deptName</th>
			</tr>
		</thead>
	</table>
	
	<br />
	<br />
	<br />
	
	
	<table id="dg" style="width:600px"></table>
	
	<script type="text/javascript">
		$('#dg').datagrid({
		    columns:[[
				{field:'empId',title:'empId'},
				{field:'empName',title:'empName'},
				{field:'gender',title:'gender'},
				{field:'email',title:'email'},
				{field:'deptName',title:'deptName'}
		    ]],
		    pagination:"true",
		    pageSize:10
		});
		
		
	</script>
	
	<script type="text/javascript">
		function getData(page, rows) {
			$.ajax({
				url:"${APP_PATH}/emps",
				data:"page=" + page,
				type:"get",
				success:function(result) {
					console.log(result);
					var data = [];
					data = {
							total:result.extend.pageInfo.total,
							rows:result.extend.pageInfo.list
							}
					console.log(data);
					$('#dg').datagrid('loadData', data);
					return data;
				}
			});
			
		}
		function pagerFilter(data){
			if (typeof data.length == 'number' && typeof data.splice == 'function'){	// is array
				data = {
					total: data.length,
					rows: data
				}
			}
			var dg = $(this);
			var opts = dg.datagrid('options');
			var pager = dg.datagrid('getPager');
			pager.pagination({
				onSelectPage:function(pageNum, pageSize){
					opts.pageNumber = pageNum;
					opts.pageSize = pageSize;
					pager.pagination('refresh',{
						pageNumber:pageNum,
						pageSize:pageSize
					});
					dg.datagrid('loadData',data);
				}
			});
			if (!data.originalRows){
				data.originalRows = (data.rows);
			}
			var start = (opts.pageNumber-1)*parseInt(opts.pageSize);
			var end = start + parseInt(opts.pageSize);
			data.rows = (data.originalRows.slice(start, end));
			return data;
		}
		
		$(function(){
			getData(1, 10);
		});
	</script>

</body>
</html>