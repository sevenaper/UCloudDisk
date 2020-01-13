import React, {Component} from 'react';
import {Menu, Icon} from 'antd';
import {Logo} from "./style";

class MyHeader extends Component {

	// state = {
	// 	current: 'mail',
	// };

	// handleClick = (e) => {
	// 	console.log(e);
	// 	this.setState({
	// 		current: e.key,
	// 	});
	// 	console.log(this.state);
	// };

	render() {
		// const SubMenu = Menu.SubMenu;
		// const MenuItemGroup = Menu.ItemGroup;

		return (
			<Menu
				// onClick={this.handleClick}
				// selectedKeys={[this.state.current]}
				mode="horizontal"
				// defaultSelectedKeys={['home']}
			>
				<Logo>U云盘</Logo>
				<Menu.Item key="mail" >
					<a href="/">
						<Icon type="home"/>Home
					</a>
				</Menu.Item>
				<Menu.Item key="app" disabled>
					<Icon type="appstore"/>App
				</Menu.Item>
				{/*<SubMenu*/}
				{/*title={<span className="submenu-title-wrapper"><Icon type="setting"/>Navigation Three - Submenu</span>}>*/}
				{/*<MenuItemGroup title="Item 1">*/}
				{/*<Menu.Item key="setting:1">Option 1</Menu.Item>*/}
				{/*<Menu.Item key="setting:2">Option 2</Menu.Item>*/}
				{/*</MenuItemGroup>*/}
				{/*<MenuItemGroup title="Item 2">*/}
				{/*<Menu.Item key="setting:3">Option 3</Menu.Item>*/}
				{/*<Menu.Item key="setting:4">Option 4</Menu.Item>*/}
				{/*</MenuItemGroup>*/}
				{/*</SubMenu>*/}
				<Menu.Item key="contact" >
					<a href="/contact">
						<Icon type="phone"/>Contact
					</a>
				</Menu.Item>
			</Menu>
		);
	}
}


export default MyHeader;
