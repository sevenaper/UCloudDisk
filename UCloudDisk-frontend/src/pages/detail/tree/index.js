import {Tree, Icon} from 'antd';
import React from 'react';
import {connect} from 'react-redux';
import * as actionCreators from "../store/actionCreators";


const DirectoryTree = Tree.DirectoryTree;
const {TreeNode} = Tree;
var tree = {};

class TreeList extends React.Component {
	onSelect = () => {
	};

	onExpand = () => {
	};


	componentDidMount() {
		// this.props.getTree(this.props.content,tree);
	}


	render() {
		console.log("abc");
		return (
			<div>

				{/*{*/}
					{/*console.log("asdf")*/}
				{/*}*/}
				<DirectoryTree
					multiple
					defaultExpandAll
					onSelect={this.onSelect}
					onExpand={this.onExpand} className="TreeListWrapper"
				>
					<TreeNode title="parent 0" key="0-0">
						<TreeNode title="leaf 0-0" key="0-0-0" isLeaf/>
						<TreeNode title="leaf 0-1" key="0-0-1" isLeaf/>
					</TreeNode>
					<TreeNode title="parent 1" key="0-1">
						<TreeNode title="parent 1" key="1-1"/>
						<TreeNode title="leaf 1-0" key="0-1-0" isLeaf/>
						<TreeNode title="leaf 1-1" key="0-1-1" isLeaf/>
					</TreeNode>
					<TreeNode title="回收站" href="/delete" icon={() => (<Icon type={'delete'}/>)}/>
				</DirectoryTree>
			</div>
		);
	}
}

const mapState = (state) => ({
	content: state.getIn(['detail', 'content']),
	files: state.getIn(['detail', 'files']),
	tree: state.getIn(['detail', 'tree']),
	isFetching: state.getIn(['detail', 'isFetching']),
});
const mapDispatch = (dispatch) => ({
	// getTree(content, tree) {
	// 	//console.log(accountElem);
	// 	dispatch(actionCreators.getTree(content, tree));
	// },
});
export default connect(mapState, mapDispatch)(TreeList);