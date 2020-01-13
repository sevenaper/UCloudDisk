/*
 * @Author: 刘鑫
 * @Date: 2018-12-08 15:51:17
 */
import React, {Component} from "react";
import {connect} from "react-redux";
import {withRouter} from "react-router-dom";
import {Layout, Input, Button, List, Skeleton, Modal} from "antd";
import "./style.css";
import UploadForm from "./upload";
import * as actionCreators from "./store/actionCreators";

const confirm = Modal.confirm;

class Detail extends Component {
	componentDidMount() {
		this.props.getDetail();
	}

	// search(value) {
	// 	console.log(searchContent);
	// 	var newContent = [];
	// 	// this.props.searchContent.map(data => {
	// 	// 	// console.log(data.get("file_name"))
	// 	// 	if (data.get("file_name").indexOf(value) !== -1) {
	// 	// 		newContent.push(data);
	// 	// 	}
	// 	// });
	// 	searchContent = newContent;
	// 	console.log(newContent);
	// }

	render() {
		const Search = Input.Search;
		const loadMore = !this.props.initLoading && !this.props.loading ? (
			<div style={{
				textAlign: 'center', marginTop: 12, height: 32, lineHeight: '32px',
			}}
			>
				<Button onClick={this.onLoadMore}>loading more</Button>
			</div>
		) : null;
		const {Content} = Layout;
		return (
			<Content style={{padding: '0 50px'}}>
				<Layout style={{padding: '24px 0', background: '#fff'}}>
					<div style={{margin: "10px"}}><UploadForm/></div>
					<Content style={{padding: '0 24px', minHeight: 280}}>
						<Search
							placeholder="input search text"
							onSearch={(value, event) => {
								this.props.search(value);
							}}
							enterButton
						/>
						<List
							className="demo-loadmore-list ListWrapper"
							loading={this.props.content == null}
							itemLayout="horizontal"
							loadMore={loadMore}
							dataSource={this.props.searchContent}
							renderItem={item => (
								<List.Item
									actions={[
										<Button
											type="danger"
											htmlType="submit"
											className="delete-button"
											style={{width: '100%'}}
											onClick={() => {
												confirm({
													title: 'Do you want to delete this item?',
													autoFocusButton: 'cancel',
													okType: 'danger',
													iconType: 'warning',
													onOk: () => {
														this.props.deleteitem(item.get("file_name"), item.get("file_path"));
													},
													onCancel() {
													},
												});
											}}
										>
											Delete
										</Button>
									]}
								>
									{

										item.get("file_hash") === "DIR" ? null : (
											<Skeleton avatar title={false} loading={item.loading} active>
												<List.Item.Meta
													//avatar={<Avatar src="https://zos.alipayobjects.com/rmsportal/ODTLcjxAfvqbxHnVXCYX.png"/>}
													// title={<a
													// 	href={require("../../statics/a.pdf")} target="_Blank">{item.get('file_path') +
													// "/" + item.get('file_name')}</a>
													// }
													title={
														<a href={encodeURIComponent("/interfaces/my_files/" + (item.get('file_path') + "/" +
															item.get("file_name")).substr(1)).replace(/%2F/g, "/")}
															//href="www.baidu.com"
															 target="_Blank"
															 rel="noopener noreferrer"
														>{item.get('file_path') +
														"/" + item.get('file_name')}</a>}

													description={"file size:  " + item.get('file_size') + "b"}/>
											</Skeleton>)
									}
								</List.Item>
							)}

						/>

					</Content>
				</Layout>
			</Content>
		);
	}
}

const mapState = (state) => ({
	initLoading: state.getIn(['detail', 'initLoading']),
	content: state.getIn(['detail', 'content']),
	all: state.getIn(['detail', 'all']),
	searchContent: state.getIn(['detail', 'searchFile'])
});
const mapDispatch = (dispatch) => ({
	getDetail() {
		dispatch(actionCreators.getDetail());
	},
	search(value) {
		dispatch(actionCreators.search(value));
	},
	deleteitem(file_name, file_path) {
		dispatch(actionCreators.deleteitem(file_name, file_path));
	}

});
export default connect(
	mapState, mapDispatch
)(withRouter(Detail));
