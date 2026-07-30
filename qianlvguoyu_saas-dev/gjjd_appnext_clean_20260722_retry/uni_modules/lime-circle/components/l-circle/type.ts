// @ts-nocheck

export interface CircleProps {
	canvasId?: string;
	/**
	* 进度环尺寸
	*/
	size : string
	/**
	 * 百分比
	 */
	percent : number
	/**
	 *  顶端形态
	 */
	lineCap : string
	/**
	 * 进度环线宽
	 */
	strokeWidth : any//number | string
	/**
	 * 进度环颜色
	 */
	strokeColor ?: any//string | string[]
	/**
	 * 轨道环线宽
	 */
	trailWidth : any//number | string
	/**
	 * 轨道环颜色
	 */
	trailColor ?: string
	/**
	 * 是否显示为仪表盘
	 */
	dashboard : boolean
	/**
	 * 是否为顺时针
	 */
	clockwise : boolean
	/**
	 * 持续时间
	 */
	duration : number
	/**
	 *  总长度
	 */
	max : number
	/**
	 * 缺口角度 
	*/
	gapDegree: number
	/**
	 * 缺口位置
	*/
	gapPosition: string
}