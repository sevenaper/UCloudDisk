import React from 'react';
import ReactDOM from 'react-dom';


class UploadForm extends React.Component {

	constructor(props) {
		super(props);
		this.state = {
			iFrameHeight: '0px'
		}
	}



	render() {
		return (
			<iframe
				title="upload"
				//style={{width:'100%', height:this.state.iFrameHeight, overflow:'visible'}}
				style={{width:'100%', paddingTop:"10px", overflow:'visible'}}
				onLoad={() => {
					const obj = ReactDOM.findDOMNode(this);
					this.setState({
						"iFrameHeight":  obj.contentWindow.document.body.scrollHeight + 'px'
					});
				}}
				ref="iframe"
				src="./upload.html"
				width="100%"
				height={this.state.iFrameHeight}
				scrolling="no"
				frameBorder="0"
			/>
		);
	}
}
export default UploadForm;