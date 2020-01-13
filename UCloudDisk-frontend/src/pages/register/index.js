/*
 * @Author: 刘鑫
 * @Date: 2018-12-09 22:27:00
 */
import React, {Component} from "react";
import {Form, Input, Checkbox, Button, Modal} from "antd";
import {connect} from "react-redux";
import {actionCreators} from "./store";
import PropTypes from "proptypes";

const FormItem = Form.Item;
let formData = {
	username: '',
	password: ''
};
let history;

class RegistrationFormWrapper extends Component {
	state = {
		confirmDirty: false,
		autoCompleteResult: [],
		username: '',
		password: '',
		visible: false,
	};

	showModal = () => {
		this.setState({
			visible: true,
		});
	};

	handleOk = () => {

		this.setState({
			visible: false,
		});
	};


	componentDidMount() {
		history = this.context.router.history;
		//console.log(history);
	}

	handleConfirmBlur = e => {
		const value = e.target.value;
		this.setState({confirmDirty: this.state.confirmDirty || !!value});
	};

	compareToFirstPassword = (rule, value, callback) => {
		const form = this.props.form;
		if (value && value !== form.getFieldValue("password")) {
			callback("Two passwords that you enter is inconsistent!");
		} else {
			callback();
		}
	};

	validateToNextPassword = (rule, value, callback) => {
		const form = this.props.form;
		if (value && this.state.confirmDirty) {
			form.validateFields(["confirm"], {force: true});
		}
		callback();
	};

	onChange = (e) => {
		this.setState({[e.target.name]: e.target.value});
	};

	render() {
		//console.log(this.state);
		formData.username = this.state.username;
		formData.password = this.state.password;
		const {getFieldDecorator} = this.props.form;
		const formItemLayout = {
			labelCol: {
				xs: {span: 24},
				sm: {span: 8}
			},
			wrapperCol: {
				xs: {span: 24},
				sm: {span: 16}
			}
		};
		const tailFormItemLayout = {
			wrapperCol: {
				xs: {
					span: 24,
					offset: 0
				},
				sm: {
					span: 16,
					offset: 8
				}
			}
		};

		return (
			<Form
				// onSubmit={()=>this.props.handleSubmit(this.state)}


				style={{
					border: "1px #aaaaaa solid",
					width: "600px",
					margin: "40px auto",
					boxShadow: "0 4px 5px rgba(0, 0, 0, 0.3)"
				}}
			>
				<p align="center">用户注册</p>
				<FormItem
					style={{width: "400px", margin: "auto"}}
					{...formItemLayout}
					label="Username"
				>
					{getFieldDecorator("email", {
						rules: [
							{
								type: "regexp",
								message: "User name can not contain @ and ."
							},
							{
								required: true,
								message: "Please input your Usernmae!"
							}
						]
					})(<Input name="username" onChange={this.onChange}/>)}
				</FormItem>
				<FormItem
					style={{width: "400px", margin: "auto"}}
					{...formItemLayout}
					label="Password"
				>
					{getFieldDecorator("password", {
						rules: [
							{
								required: true,
								message: "Please input your password!"
							},
							{
								validator: this.validateToNextPassword
							}
						]
					})(
						<Input type="password" name="password" onChange={this.onChange}/>
					)}
				</FormItem>
				<FormItem
					style={{width: "400px", margin: "auto"}}
					{...formItemLayout}
					label="Confirm Password"
				>
					{getFieldDecorator("confirm", {
						rules: [
							{
								required: true,
								message: "Please confirm your password!"
							},
							{
								validator: this.compareToFirstPassword
							}
						]
					})(<Input type="password" onBlur={this.handleConfirmBlur}/>)}
				</FormItem>
				<FormItem {...tailFormItemLayout} style={{margin: "auto"}}>
					{getFieldDecorator("agreement", {
						valuePropName: "checked"
					})(
						<Checkbox>
							I have read the <a onClick={this.showModal}>agreement</a>
						</Checkbox>
					)}
				</FormItem>
				<Modal
					title="用户注册须知"
					visible={this.state.visible}
					footer={null}
					onCancel={this.handleOk}
				>
					U云盘平台是由武汉大学计算机学院计算机科学与技术系学生的一个项目。为了充分保障您的合法权益、向您提供更好的服务，请先阅读以下内容，同意后方可注册。
					<p>保护用户隐私是本平台的一项基本政策，本平台保证不对外公开或向第三方提供用户注册资料及用户在使用本平台提供服务时存储在本平台的非公开内容，但下列情况除外：</p>
					<p/>a）事先获得用户的明确授权；
					<br/>b）根据有关的法律法规要求；
					<br/>c）按照相关政府主管部门的要求；
					<br/>d）为维护社会公众的利益；
					<br/>e）为维护本平台的合法权益；
					<p>如因系统维护或升级的需要而需暂停本平台的服务，本平台将尽可能事先进行通告，但本平台同时保留在不事先通知用户的情况下随时中断或终止部分或全部服务的权利。</p>
					<p>最后，该产品的最终解释权归U云盘公司所有</p>
				</Modal>
				<p align="center" style={{color: "red", fontSize: "12px"}}>{
					this.props.error_code === 1001 ? '用户名只能由字母、数字、下划线组成，开头必须是字母，不能超过16位' :
						this.props.error_code === 1002 ? '用户名已存在' : ''
				}</p>

				<FormItem
					style={{width: "200px", margin: "auto"}}
					{...tailFormItemLayout}
				>
					<Button type="primary" htmlType="submit"
									onClick={() => {
										this.props.handleSubmit(formData.username, formData.password, history);
									}
									}>
						Register
					</Button>
				</FormItem>
			</Form>

		);
	}
}

const mapState = (state) => {
	return {
		error_code: state.getIn((['register', 'error_code']))
	};
};
const mapDispatch = (dispatch) => {
	return {
		handleSubmit(username, password, history) {
			// e.preventDefault();
			// console.log(e.target);
			dispatch(actionCreators.userSignupRequest(username, password, history));
		},

	};
};
RegistrationFormWrapper.contextTypes = {
	router: PropTypes.object.isRequired
};
const RegistrationForm = Form.create()(RegistrationFormWrapper);
export default connect(
	mapState,
	mapDispatch
)(RegistrationForm);
