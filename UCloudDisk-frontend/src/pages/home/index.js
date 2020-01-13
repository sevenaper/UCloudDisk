import React, {Component, Fragment} from 'react';
import "./style.css";
import "./form";
import Loginform from './form';
import {Carousel} from 'antd';

class Home extends Component {


	render() {

		return (
			<Fragment>

				<Carousel autoplay>
					<div className="picWrapper1"/>
					<div className="picWrapper2"/>
					<div className="picWrapper3"/>
					<div className="picWrapper4"/>
				</Carousel>
				<div id="login-download">
					<div id="windows">
						<i className="iconfont icon-windows"/>
						<br/>
						<br/>
						Windows
					</div>
					<div id="mac" >
						<i className="iconfont icon-mac"/>
						<br/>
						<br/>
						MAC
					</div>
					<div id="ios">
						<i className="iconfont icon-android"/>
						<br/>
						<br/>
						IOS
					</div>
					<div id="android">
						<i className="iconfont icon-iOS"/>
						<br/>
						<br/>
						Android
					</div>
				</div>
				<div className="loginFormWrapper"><Loginform/></div>


			</Fragment>


		);
	}
}

export default Home
