import {
	Form, Icon, Input, Button, Checkbox,
} from 'antd';
import * as actionCreators from './store/actionCreators';
import React from 'react';
import {connect} from 'react-redux';
import './style.css';
import PropTypes from "proptypes";
import cookie from 'react-cookies'

const FormItem = Form.Item;
let formData = {
	username: '',
	password: ''
};
let history;

class NormalLoginForm extends React.Component {
	state = {
		judgeLogin: false,
		username: '',
		password: ''
	};
	onChange = (e) => {
		this.setState({[e.target.name]: e.target.value});
	};

	componentDidMount() {
		history = this.context.router.history

	}
	render() {
		const {getFieldDecorator} = this.props.form;
		formData.username = this.state.username;
		formData.password = this.state.password;
		return (
			<Form className="login-form">
				U云盘登陆
				<br/><br/>
				<FormItem>
					{getFieldDecorator('userName', {
						rules: [{required: true, message: 'Please input your username!'}],
					})(
						<Input prefix={<Icon type="user" style={{color: 'rgba(0,0,0,.25)'}}/>} name="username"
									 placeholder="Username" onChange={this.onChange}/>
					)}
				</FormItem>
				<FormItem>
					{getFieldDecorator('password', {
						rules: [{required: true, message: 'Please input your Password!'}],
					})(
						<Input prefix={<Icon type="lock" style={{color: 'rgba(0,0,0,.25)'}}/>} type="password" name="password"
									 placeholder="Password" onChange={this.onChange}/>
					)}
				</FormItem>
				<FormItem>
					{getFieldDecorator('remember', {
						valuePropName: 'checked',
						initialValue: true,
					})(
						<Checkbox>Remember me</Checkbox>
					)}
					<a className="login-form-forgot" href="" style={{float: 'right'}}>Forgot password</a>
					<br/>
					<Button type="primary" htmlType="submit" className="login-form-button" style={{width: '100%'}}
									onClick={() => this.props.handleSubmit(formData.username, formData.password, history)}>
						Log in
					</Button>
					Or <a href="/register">register now!</a>

					<div align="left" style={{color: "red",fontSize:"12px"}}>{
						this.props.error_type === 1 ? '该用户不存在' :
							this.props.error_type === 2 ? '密码错误' : ''
					}</div>


				</FormItem>
			</Form>
		);
	}
}

const mapState = (state) => ({
	error_type: state.getIn(['login', 'error_type']),
	loginStatus: state.getIn(['login', 'login'])
});

const mapDispatch = (dispatch) => ({
	handleSubmit(accountElem, passwordElem, history) {
		//console.log(accountElem);
		dispatch(actionCreators.login(accountElem, passwordElem, history));
		cookie.save(accountElem, passwordElem);
	},
	logout() {
		dispatch(actionCreators.logout());
	}
});
NormalLoginForm.contextTypes = {
	router: PropTypes.object.isRequired
};
const LoginForm = Form.create()(NormalLoginForm);
export default connect(mapState, mapDispatch)(LoginForm);