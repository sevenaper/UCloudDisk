var filePaths = [
	"a",
	"a/b",

];
var tree = {};
filePaths.forEach(function (path) {
	var currentNode = tree;
	path.split('/').forEach(function (segment) {
		if (currentNode[segment] === undefined) {
			currentNode[segment] = {};
		}
		currentNode = currentNode[segment];
	});
});

// Now we have a tree represented as nested dictionaries.
console.log(JSON.stringify(tree, null, 2));
function toTreeData(tree) {
	return Object.keys(tree).map(function (title) {
		var o = { title: title };
		if (Object.keys(tree[title]).length > 0) {
			o.children = toTreeData(tree[title]);
		}

		return o;
	});
}

console.log(JSON.stringify(toTreeData(tree), null, 2));