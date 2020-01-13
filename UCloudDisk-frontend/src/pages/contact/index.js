import React, {Component} from "react";
import {ParallaxProvider, Parallax} from "react-skrollr";
import './style.css';

const data = {
	"data-top-top": "transform: translateX(-100%);",
	"data-center-center": "opacity: 1;transform: translateX(0%);",
	"data-bottom-top": "opacity: 0;"
};

class Contact extends Component {
	render() {
		return (
			<ParallaxProvider
				init={{
					smoothScrollingDuration: 1000,
					smoothScrolling: true,
					forceHeight: false
				}}
				getScrollTop={scrollTop => console.log("scrollTop", scrollTop)}
			>
				<div className="title">U云盘成员简介</div>


				<Parallax data={data}>
					<div className="span">刘鑫 学号:2016301580034 {"\u2728"}</div>
				</Parallax>
				<Parallax data={data}>
					<div className="span">黄思达 学号:2016302330057 {"\u2728"}</div>
				</Parallax>
				<Parallax data={data}>
					<div className="span">孟天航 2016301750053 {"\u2728"}</div>
				</Parallax>
				<Parallax data={data}>
					<div className="span">滕正丽 学号:2016301500350 {"\u2728"}</div>
				</Parallax>
				<Parallax data={data}>
				<div className="span">刘天义 学号:2016301510044 {"\u2728"}</div>
				</Parallax>
				<Parallax data={data}>
					<div className="span">钟雨村 学号:2016302180129 {"\u2728"}</div>
				</Parallax>
				<div className="githubWrapper">
					<a href="https://github.com/WHUbigsoft/CloudDisk_FrontEnd" target="_blank"
						 rel="noopener noreferrer">
						<div className="github">
							Github
						</div>
					</a></div>


			</ParallaxProvider>
		);
	}
}

export default Contact;