package com.cmc.monitor.entity;

import java.io.Serializable;

/**
 * Cable modem termination system
 * @author richard
 *
 */
public class Cmts implements Serializable {

	// serialVersionUID
	private static final long serialVersionUID = -6570345078742799747L;
	
	private long cmtsId;
	private String title;
	private String host;
	private String community;
	private boolean enable = true;;
	
	public Cmts() {
	}

	public long getCmtsId() {
		return cmtsId;
	}

	public void setCmtsId(long cmtsId) {
		this.cmtsId = cmtsId;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getCommunity() {
		return community;
	}

	public void setCommunity(String community) {
		this.community = community;
	}
	
	public boolean isEnable() {
		return enable;
	}
	
	public void setEnable(boolean enable) {
		this.enable = enable;
	}

	@Override
	public String toString() {
		return "Cmts [cmtsId=" + cmtsId + ", title=" + title + ", host=" + host
				+ ", community=" + community + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (cmtsId ^ (cmtsId >>> 32));
		result = prime * result
				+ ((community == null) ? 0 : community.hashCode());
		result = prime * result + ((host == null) ? 0 : host.hashCode());
		result = prime * result + ((title == null) ? 0 : title.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Cmts other = (Cmts) obj;
		if (cmtsId != other.cmtsId) {
			return false;
		}
		if (community == null) {
			if (other.community != null) {
				return false;
			}
		} else if (!community.equals(other.community)) {
			return false;
		}
		if (host == null) {
			if (other.host != null) {
				return false;
			}
		} else if (!host.equals(other.host)) {
			return false;
		}
		if (title == null) {
			if (other.title != null) {
				return false;
			}
		} else if (!title.equals(other.title)) {
			return false;
		}
		return true;
	}

}
