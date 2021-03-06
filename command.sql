
CREATE TABLE [dbo].[CFG_TXN_GROUP](
	[ID] [varchar](36) NOT NULL,
	[GROUP_CODE] [varchar](30) NULL,
	[GROUP_NAME] [nvarchar](50) NULL,
	[MAIN_ID] [varchar](36) NULL,
	[HOST_ID] [varchar](36) NULL,
	[GROUP_STATUS] [varchar](1) NULL,
	[EFFECT_TYPE] [varchar](1) NULL,
	[EFFECT_TIME] [datetime] NULL,
	[MEMO] [nvarchar](1000) NULL,
	[CREATED_USER] [varchar](20) NULL,
	[CREATED_TIME] [datetime] NULL,
	[UPDATED_USER] [varchar](20) NULL,
	[UPDATED_TIME] [datetime] NULL,
 CONSTRAINT [PK_CFG_TXN_GROUP] PRIMARY KEY CLUSTERED 
(
	[ID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]



CREATE TABLE [dbo].[CFG_SERVICE](
	[ID] [varchar](36) NOT NULL,
	[SERVICE_CODE] [varchar](30) NULL,
	[SERVICE_NAME] [nvarchar](50) NULL,
	[MAIN_ID] [varchar](36) NULL,
	[VERSION] [varchar](10) NULL,
	[SERVICE_STATUS] [varchar](1) NULL,
	[EFFECT_TYPE] [varchar](1) NULL,
	[EFFECT_TIME] [datetime] NULL,
	[MEMO] [nvarchar](1000) NULL,
	[CREATED_USER] [varchar](20) NULL,
	[CREATED_TIME] [datetime] NULL,
	[UPDATED_USER] [varchar](20) NULL,
	[UPDATED_TIME] [datetime] NULL,
 CONSTRAINT [PK_CFG_SERVICE] PRIMARY KEY CLUSTERED 
(
	[ID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
